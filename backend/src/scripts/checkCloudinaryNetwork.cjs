// Unsigned, credential-free diagnostics. No configuration file, auth header, or URL query is used.
const dns = require('dns').promises;
const https = require('https');
const { diagnoseError } = require('./checkTestMedia.cjs');
async function run() {
  if (process.env.NODE_TLS_REJECT_UNAUTHORIZED === '0') {
    console.log(JSON.stringify({ code: 'INSECURE_TLS_OVERRIDE', httpStatus: null, message: 'Remove the TLS-disable override; no request sent.' }));
    process.exitCode = 3; return;
  }
  try {
    const addresses = await dns.lookup('api.cloudinary.com', { all: true });
    console.log(JSON.stringify({ code: 'DNS_OK', httpStatus: null, message: `api.cloudinary.com resolved (${addresses.length} addresses).` }));
  } catch (error) { console.log(JSON.stringify(diagnoseError(error))); process.exitCode = 5; return; }
  await new Promise(resolve => {
    const request = https.request({ hostname: 'api.cloudinary.com', port: 443, path: '/', method: 'HEAD',
      rejectUnauthorized: true, timeout: 15000 }, response => {
      response.resume();
      console.log(JSON.stringify({ code: 'HTTPS_REACHABLE', httpStatus: response.statusCode,
        message: 'Cloudinary API responded over validated TLS. This unsigned probe does not test credentials.' }));
      resolve();
    });
    request.on('timeout', () => request.destroy(Object.assign(new Error('timeout'), { code: 'ETIMEDOUT' })));
    request.on('error', error => { console.log(JSON.stringify(diagnoseError(error))); process.exitCode = 5; resolve(); });
    request.end();
  });
  const proxy = ['HTTP_PROXY','HTTPS_PROXY','ALL_PROXY'].some(k => !!process.env[k]);
  console.log(JSON.stringify({ code: 'LOCAL_TLS_SETTINGS', httpStatus: null,
    message: `TLS validation enabled; proxy environment ${proxy ? 'present' : 'absent'}; extra CA ${process.env.NODE_EXTRA_CA_CERTS ? 'configured' : 'not configured'}. No credential values inspected.` }));
}
run().catch(() => { console.log(JSON.stringify({ code: 'DIAGNOSTIC_EXECUTION_FAILED', httpStatus: null, message: 'Network diagnostic execution failed.' })); process.exitCode = 3; });
