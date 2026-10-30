const path = require('path');
const { spawnSync } = require('child_process');
const { checkTestMedia } = require('../src/scripts/checkTestMedia.cjs');
const environment = { CLOUDINARY_CLOUD_NAME: 'test-fixture-cloud', CLOUDINARY_API_KEY: 'test-fixture-key', CLOUDINARY_API_SECRET: 'test-fixture-secret' };
const client = () => ({ config: jest.fn(), api: { ping: jest.fn() } });
test('helper executes as a real file without inline JavaScript quoting', () => {
  const result = spawnSync(process.execPath, [path.join(__dirname, '../src/scripts/checkTestMedia.cjs')],
    { encoding: 'utf8', env: { ...process.env, CLOUDINARY_CLOUD_NAME: '', CLOUDINARY_API_KEY: '', CLOUDINARY_API_SECRET: '' } });
  expect(result.status).toBe(2); expect(result.stderr).toContain('configuration is incomplete');
  expect(result.stderr).not.toContain('SyntaxError');
});
test('successful ping passes settings through config and confirms success', async () => {
  const c = client(); c.api.ping.mockResolvedValue({ status: 'ok' });
  const result = await checkTestMedia(environment, () => c);
  expect(result.exitCode).toBe(0); expect(c.config).toHaveBeenCalledWith(expect.objectContaining({ api_secret: environment.CLOUDINARY_API_SECRET }));
  expect(c.api.ping).toHaveBeenCalledWith({ timeout: 15000 });
  expect(result.message).not.toContain(environment.CLOUDINARY_API_SECRET);
});

test('installed v2 adapter forwards valid options to v1 without network access', async () => {
  const sdk = require('cloudinary');
  const ping = jest.spyOn(sdk.api, 'ping').mockImplementation((callback, options) => {
    expect(options).toEqual({ timeout: 15000 });
    return Promise.resolve({ status: 'ok' });
  });
  try { expect((await checkTestMedia(environment, () => sdk.v2)).exitCode).toBe(0); }
  finally { ping.mockRestore(); }
});

test.each([
  ['timeout secret https://signed.invalid', 'ETIMEDOUT', 'StringError'],
  [new TypeError('Cannot read properties of null (reading content_type)'), 'CHECKER_EXECUTION_ERROR', 'TypeError'],
  [{ error: 'timeout secret', cause: { code: 'ECONNRESET' } }, 'ECONNRESET', 'ProviderError']
])('sanitizes string, Error and sibling causes', async (error, code, name) => {
  const c = client(); c.api.ping.mockRejectedValue(error);
  const result = await checkTestMedia(environment, () => c);
  expect(result.diagnostic).toMatchObject({ code, name, httpStatus: null });
  expect(JSON.stringify(result)).not.toMatch(/signed.invalid|content_type|timeout secret/);
});
test('SDK execution failure is not labeled authentication failure', async () => {
  const result = await checkTestMedia(environment, () => { throw new SyntaxError('fixture execution error'); });
  expect(result.exitCode).toBe(3); expect(result.message).toContain('authentication was not tested');
});
test('actual rejection is distinct and credential details are never echoed', async () => {
  const c = client(); c.api.ping.mockRejectedValue(Object.assign(new Error('Unknown key ' + environment.CLOUDINARY_API_SECRET), { http_code: 401 }));
  const result = await checkTestMedia(environment, () => c);
  expect(result.exitCode).toBe(4); expect(result.message).not.toContain(environment.CLOUDINARY_API_SECRET);
});
test('network failure does not claim credentials are invalid', async () => {
  const c = client(); c.api.ping.mockRejectedValue(new Error('timeout'));
  expect((await checkTestMedia(environment, () => c)).exitCode).toBe(5);
});
test('nested SDK authentication errors are not mislabeled as network failures', async () => {
  const c = client(); c.api.ping.mockRejectedValue({ request_options: { auth: 'fixture-key:fixture-secret' },
    error: { http_code: 401, message: 'Unknown API key fixture-key https://signed.invalid/?signature=fixture-secret' } });
  const result = await checkTestMedia(environment, () => c);
  expect(result.exitCode).toBe(4); expect(result.diagnostic.httpStatus).toBe(401);
  expect(result.diagnostic.code).toBe('CLOUDINARY_HTTP_401');
  expect(JSON.stringify(result)).not.toMatch(/fixture-key|fixture-secret|signed.invalid|request_options/);
});
test.each(['ENOTFOUND','ETIMEDOUT','SELF_SIGNED_CERT_IN_CHAIN'])('classifies %s without echoing request details', async code => {
  const c = client(); c.api.ping.mockRejectedValue({ cause: { code, message: 'redacted fixture details' } });
  const result = await checkTestMedia(environment, () => c);
  expect(result.exitCode).toBe(5); expect(result.diagnostic.code).toBe(code); expect(result.diagnostic.httpStatus).toBeNull();
});
test('refuses a TLS-disable override without sending credentials', async () => {
  const c = client();
  const result = await checkTestMedia({ ...environment, NODE_TLS_REJECT_UNAUTHORIZED: '0' }, () => c);
  expect(result.exitCode).toBe(3); expect(c.api.ping).not.toHaveBeenCalled();
});

test('provider permission text is distinguished even when HTTP status is 401', async () => {
  const c = client(); c.api.ping.mockRejectedValue({ error: { http_code: 401,
    message: 'API key lacks permission for admin API secret-fixture' } });
  const result = await checkTestMedia(environment, () => c);
  expect(result.exitCode).toBe(4);
  expect(result.diagnostic).toMatchObject({ code: 'CLOUDINARY_ACCESS_REJECTED', httpStatus: 401 });
  expect(JSON.stringify(result)).not.toContain('secret-fixture');
});

test.each([
  ['Server return invalid JSON response. Status Code 401', 'CLOUDINARY_NON_JSON_RESPONSE'],
  ['Authorization required', 'CLOUDINARY_AUTHORIZATION_REQUIRED']
])('retains safe classification of generic HTTP 401 responses', async (message, code) => {
  const c = client(); c.api.ping.mockRejectedValue({ error: { http_code: 401, message } });
  expect((await checkTestMedia(environment, () => c)).diagnostic).toMatchObject({ code, httpStatus: 401 });
});

test('rejects hidden copy/paste padding without transmitting credentials', async () => {
  const c = client();
  expect((await checkTestMedia({ ...environment, CLOUDINARY_API_SECRET: ' fixture-secret ' }, () => c)).exitCode).toBe(2);
  expect(c.api.ping).not.toHaveBeenCalled();
});

test('provider message reveals reason while redacting credentials, auth and signed URLs', async () => {
  const c = client(); c.api.ping.mockRejectedValue({ error: { http_code: 401,
    message: `Rejected ${environment.CLOUDINARY_API_KEY} ${environment.CLOUDINARY_API_SECRET} ${environment.CLOUDINARY_CLOUD_NAME} https://example.invalid/?signature=sensitive Basic abc123 authorization=xyz` } });
  const result = await checkTestMedia(environment, () => c);
  expect(result.diagnostic.providerMessage).toContain('Rejected');
  expect(JSON.stringify(result)).not.toMatch(/test-fixture|example.invalid|sensitive|abc123|xyz/);
});
