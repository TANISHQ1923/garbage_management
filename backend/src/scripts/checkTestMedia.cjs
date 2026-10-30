// Credentials arrive only through inherited environment variables. Never load .env or print errors verbatim.
function safeProviderMessage(error, environment) {
  const pending = [error]; const seen = new Set(); let message = '';
  while (pending.length && seen.size < 20) {
    const item = pending.shift();
    if (typeof item === 'string') { message = item; break; }
    if (!item || typeof item !== 'object' || seen.has(item)) continue;
    seen.add(item);
    // Prefer the SDK's nested provider error over an outer wrapper message.
    if (item.error) pending.unshift(item.error);
    else if (typeof item.message === 'string') { message = item.message; break; }
    if (item.cause) pending.push(item.cause);
  }
  for (const key of ['CLOUDINARY_API_SECRET','CLOUDINARY_API_KEY','CLOUDINARY_CLOUD_NAME']) {
    const value = environment[key];
    if (!value) continue;
    for (const variant of [value, encodeURIComponent(value), Buffer.from(value).toString('base64')]) {
      message = message.split(variant).join('[REDACTED]');
    }
  }
  const basic = Buffer.from(`${environment.CLOUDINARY_API_KEY || ''}:${environment.CLOUDINARY_API_SECRET || ''}`).toString('base64');
  message = message.split(basic).join('[REDACTED]');
  return message.replace(/https?:\/\/[^\s<>"']+/gi, '[URL REDACTED]')
    .replace(/\b(?:basic|bearer)\s+[^\s,;]+/gi, '[AUTH REDACTED]')
    .replace(/(?:authorization|api[_ -]?key|api[_ -]?secret|signature)\s*[:=]\s*[^\s,;]+/gi, '[AUTH FIELD REDACTED]')
    .replace(/[\x00-\x1f\x7f]/g, ' ').slice(0, 500) || 'No provider message available.';
}
function diagnoseError(error) {
  const nodes = []; const strings = []; const seen = new Set(); const pending = [error];
  while (pending.length && nodes.length + strings.length < 20) {
    const next = pending.shift();
    if (typeof next === 'string') { strings.push(next); continue; }
    if (!next || typeof next !== 'object' || seen.has(next)) continue;
    nodes.push(next); seen.add(next);
    pending.push(next.error, next.cause);
  }
  const httpStatus = nodes.map(e => Number(e.http_code || e.statusCode || e.status)).find(s => s >= 400 && s <= 599) || null;
  const allowedCodes = new Set(['ENOTFOUND','EAI_AGAIN','ETIMEDOUT','ESOCKETTIMEDOUT','ECONNRESET','ECONNREFUSED','ENETUNREACH','EHOSTUNREACH',
    'CERT_HAS_EXPIRED','ERR_TLS_CERT_ALTNAME_INVALID','DEPTH_ZERO_SELF_SIGNED_CERT','SELF_SIGNED_CERT_IN_CHAIN',
    'UNABLE_TO_VERIFY_LEAF_SIGNATURE','UNABLE_TO_GET_ISSUER_CERT_LOCALLY']);
  const networkCode = nodes.map(e => e.code).find(c => allowedCodes.has(c));
  // Inspect provider text only for classification. Never echo it: it may contain a key, auth header, or signed URL.
  const text = [...strings, ...nodes.map(e => typeof e.message === 'string' ? e.message : '')].join(' ').toLowerCase();
  const safeNames = new Set(['Error','TypeError','SyntaxError','RangeError','ReferenceError','AggregateError']);
  const name = nodes.map(e => e.name).find(n => safeNames.has(n)) || (typeof error === 'string' ? 'StringError' : 'ProviderError');
  let code = networkCode || (httpStatus ? `CLOUDINARY_HTTP_${httpStatus}` : 'CHECK_FAILED');
  let message;
  if (/permission|not allowed|access denied|not authorized to|insufficient.*(privilege|scope)|disabled.*(api|key)|(api|key).*disabled/.test(text)) {
    code = 'CLOUDINARY_ACCESS_REJECTED';
    message = 'The provider reports an access restriction or disabled key. Check key status and Admin API access in the test environment; HTTP status alone does not establish which restriction applies.';
  }
  else if (/unknown api key|invalid api key/.test(text)) message = 'Cloudinary does not recognize the API key for the selected test environment.';
  else if (/unknown cloud|invalid cloud|cloud name.*invalid|cloud_name.*invalid/.test(text)) message = 'Cloudinary does not recognize the selected test cloud name.';
  else if (/invalid signature|invalid credentials/.test(text)) message = 'Cloudinary rejected key/secret authentication for the selected test environment.';
  else if (/invalid json response/.test(text)) {
    code = 'CLOUDINARY_NON_JSON_RESPONSE';
    message = 'The API returned a non-JSON response. The SDK discarded the body; no provider-specific authentication or permission reason is available.';
  }
  else if (/authorization required|authentication required|unauthorized/.test(text)) {
    code = 'CLOUDINARY_AUTHORIZATION_REQUIRED';
    message = 'The provider reports authorization is required; it did not identify a missing permission or a specific credential mismatch.';
  }
  else if (httpStatus === 401) message = 'Cloudinary rejected test-environment authentication; no recognized provider reason was supplied.';
  else if (httpStatus === 403) message = 'Cloudinary denied access to the ping endpoint; check test-environment API permissions.';
  else if (httpStatus === 400 || httpStatus === 404) message = 'Cloudinary rejected the cloud configuration or ping request; the API was reached.';
  else if (httpStatus === 429 || httpStatus === 420) message = 'Cloudinary returned a rate-limit response; this is not a TLS or DNS failure.';
  else if (httpStatus) message = 'Cloudinary returned an HTTP service error; the API was reached.';
  else if (['ENOTFOUND','EAI_AGAIN'].includes(networkCode)) message = 'DNS resolution of the Cloudinary API failed.';
  else if (['ETIMEDOUT','ESOCKETTIMEDOUT'].includes(networkCode) || /timed? ?out|timeout/.test(text)) { code = networkCode || 'ETIMEDOUT'; message = 'The Cloudinary API request timed out.'; }
  else if (networkCode?.includes('CERT') || networkCode?.includes('ISSUER') || networkCode?.includes('SELF_SIGNED') || /certificate|tls|ssl/.test(text)) message = 'TLS certificate validation failed. Check trusted CA/proxy configuration; do not disable validation.';
  else if (networkCode) message = 'The connection to the Cloudinary API failed before an HTTP response.';
  else if (['TypeError','SyntaxError','ReferenceError','RangeError'].includes(name)) { code = 'CHECKER_EXECUTION_ERROR'; message = 'A local SDK/checker execution error occurred; authentication was not tested.'; }
  else message = 'The checker failed without a recognizable HTTP/network error; authentication is unconfirmed.';
  return { name, code, httpStatus, message };
}
async function checkTestMedia(environment, getClient = () => require('cloudinary').v2) {
  const names = ['CLOUDINARY_CLOUD_NAME', 'CLOUDINARY_API_KEY', 'CLOUDINARY_API_SECRET'];
  if (names.some(name => !environment[name]?.trim())) return { exitCode: 2, message: 'Test storage configuration is incomplete. All three hidden settings are required.' };
  if (names.some(name => environment[name] !== environment[name].trim() || /[\r\n]/.test(environment[name]))) {
    return { exitCode: 2, message: 'A hidden setting contains surrounding whitespace or a newline. Re-enter the value without padding; no request was sent.' };
  }
  if (environment.NODE_TLS_REJECT_UNAUTHORIZED === '0') return { exitCode: 3, message: 'TLS validation is disabled in this process. Remove that override before checking test storage.', diagnostic: { code: 'INSECURE_TLS_OVERRIDE', httpStatus: null, message: 'No credential request was sent.' } };
  let client;
  try {
    client = getClient();
    client.config({ cloud_name: environment.CLOUDINARY_CLOUD_NAME, api_key: environment.CLOUDINARY_API_KEY,
      api_secret: environment.CLOUDINARY_API_SECRET, secure: true, hide_sensitive: true, debug: false,
      upload_prefix: 'https://api.cloudinary.com' });
  } catch (_) {
    return { exitCode: 3, message: 'Test storage checker could not execute. Check the installed Cloudinary SDK; authentication was not tested.' };
  }
  try {
    // v2 takes options first; v1's callback-first order causes a local TypeError.
    const response = await client.api.ping({ timeout: 15000 });
    if (response?.status !== 'ok') return { exitCode: 3, message: 'Test storage checker received an unexpected response; configuration was not saved.' };
    return { exitCode: 0, message: 'Test storage authentication succeeded; no photo uploaded.' };
  } catch (error) {
    const diagnostic = diagnoseError(error);
    if (diagnostic.code === 'CLOUDINARY_HTTP_401' && /no recognized provider reason/.test(diagnostic.message)) {
      diagnostic.providerMessage = safeProviderMessage(error, environment);
    }
    if (diagnostic.code === 'CHECKER_EXECUTION_ERROR') return { exitCode: 3, message: diagnostic.message, diagnostic };
    if ([400,401,403,404].includes(diagnostic.httpStatus)) return { exitCode: 4, message: diagnostic.message, diagnostic };
    return { exitCode: 5, message: diagnostic.message, diagnostic };
  }
}
if (require.main === module) {
  for (const name of ['CLOUDINARY_CLOUD_NAME', 'CLOUDINARY_API_KEY', 'CLOUDINARY_API_SECRET']) {
    console.log(`${name}: ${process.env[name]?.trim() ? 'present' : 'missing'}`);
  }
  checkTestMedia(process.env).then(result => {
  (result.exitCode ? console.error : console.log)(result.message);
  if (result.diagnostic) console.error(JSON.stringify(result.diagnostic));
  // Persist only our allowlisted diagnostic, never the provider object or request details.
  if (process.env.TEST_MEDIA_DIAGNOSTIC_PATH) {
    try {
      require('fs').writeFileSync(process.env.TEST_MEDIA_DIAGNOSTIC_PATH,
        JSON.stringify({ exitCode: result.exitCode, diagnostic: result.diagnostic || null }) + '\n', { mode: 0o600 });
    } catch (_) { console.error('Sanitized diagnostic could not be saved.'); }
  }
  process.exitCode = result.exitCode;
}).catch(() => {
  console.error('Test storage checker execution failed before authentication could be confirmed.');
  process.exitCode = 3;
});
}
module.exports = { checkTestMedia, diagnoseError, safeProviderMessage };
