const DEFAULT_CONFIG = Object.freeze({ emptyDistanceCm: 15.9, minDistanceCm: 2,
  fullThreshold: 80, emptyThreshold: 20, filterSamples: 3, confirmReadings: 3,
  confirmDurationMs: 10000, maxGapMs: 6000, maxReadingAgeMs: 10000,
  staleAfterMs: 15000, offlineAfterMs: 60000, retryDelayMs: 15000 });

function validConfig(c) {
  return Object.keys(DEFAULT_CONFIG).every(k => Number.isFinite(c[k]) && c[k] > 0)
    && c.emptyDistanceCm > c.minDistanceCm && c.emptyThreshold < c.fullThreshold && c.fullThreshold <= 100
    && c.minDistanceCm <= c.emptyDistanceCm * (1 - c.fullThreshold / 100)
    && c.emptyThreshold < 100 && c.filterSamples % 2 === 1 && Number.isInteger(c.filterSamples) && c.filterSamples <= 15
    && Number.isInteger(c.confirmReadings) && c.confirmReadings >= 2 && c.confirmReadings <= 100
    && c.offlineAfterMs > c.staleAfterMs && c.staleAfterMs >= c.maxReadingAgeMs;
}
const distanceValid = (distance, config) => typeof distance === 'number' && Number.isFinite(distance)
  && distance >= config.minDistanceCm && distance <= config.emptyDistanceCm;
const fillFor = (distance, c) => 100 * (c.emptyDistanceCm - distance) / c.emptyDistanceCm;

// Pure state reducer. Invalid/gapped/reversed samples cannot extend a confirmation streak.
function evaluate(previous, reading, config) {
  const state = { armed: true, samples: [], ...previous };
  state.samples = [...state.samples];
  if (!reading.valid || !distanceValid(reading.distanceCm, config)) {
    state.samples = []; state.candidate = null; state.count = 0; state.since = null;
    state.lastAt = reading.capturedAtMs;
    return { state, valid: false, full: false, empty: false };
  }
  if (state.lastAt == null || reading.capturedAtMs - state.lastAt > config.maxGapMs) {
    state.samples = []; state.candidate = null; state.count = 0; state.since = null;
  }
  state.lastAt = reading.capturedAtMs;
  state.samples.push(reading.distanceCm); state.samples = state.samples.slice(-config.filterSamples);
  const median = [...state.samples].sort((a, b) => a - b)[Math.floor(state.samples.length / 2)];
  const fillPercent = fillFor(median, config), rawFill = fillFor(reading.distanceCm, config);
  const candidate = state.samples.length < config.filterSamples ? null
    : (fillPercent + 1e-9 >= config.fullThreshold && rawFill + 1e-9 >= config.fullThreshold ? 'FULL'
      : (fillPercent <= config.emptyThreshold && rawFill <= config.emptyThreshold ? 'EMPTY' : null));
  if (candidate && candidate === state.candidate) state.count++;
  else { state.candidate = candidate; state.count = candidate ? 1 : 0; state.since = candidate ? reading.capturedAtMs : null; }
  const confirmed = candidate && state.count >= config.confirmReadings && reading.capturedAtMs - state.since >= config.confirmDurationMs;
  if (candidate !== 'EMPTY') state.emptyLatched = false;
  const empty = !!confirmed && candidate === 'EMPTY' && !state.emptyLatched;
  if (empty) { state.armed = true; state.emptyLatched = true; }
  return { state, valid: true, fillPercent, full: !!confirmed && candidate === 'FULL', empty };
}
function healthFor(bin, now = Date.now()) {
  if (!bin.lastContactAt || now - bin.lastContactAt > bin.config.offlineAfterMs) return 'OFFLINE';
  if (!bin.lastMeasurementAt || now - bin.lastMeasurementAt > bin.config.staleAfterMs) return 'STALE';
  if (!bin.lastReadingValid) return 'FAULT';
  if (!bin.lastValidAt || now - bin.lastValidAt > bin.config.staleAfterMs) return 'STALE';
  return 'ONLINE';
}
module.exports = { DEFAULT_CONFIG, validConfig, distanceValid, fillFor, evaluate, healthFor };
