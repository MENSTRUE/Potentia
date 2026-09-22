const TARGET_FOLDER_ID = '1sfTUFFtuy0p9pqt5X8AaCV6BqTJe_2eF';
const SPREADSHEET_NAME = 'POTENTIA Pilot Live Data';
const SESSION_CSV_BACKUP_NAME = 'potentia_pilot_sessions_live.csv';
const RESPONSE_CSV_BACKUP_NAME = 'potentia_pilot_responses_live.csv';

const SESSION_HEADERS = [
  'received_at',
  'participant_id',
  'session_id',
  'consent_version',
  'app_version',
  'started_at',
  'completed_at',
  'assessment_version',
  'scoring_version',
  'language',
  'logical_score',
  'creative_score',
  'verbal_score',
  'spatial_score',
  'social_score',
  'practical_score',
  'response_count'
];

const RESPONSE_HEADERS = [
  'received_at',
  'participant_id',
  'session_id',
  'consent_version',
  'app_version',
  'started_at',
  'completed_at',
  'assessment_version',
  'scoring_version',
  'language',
  'item_id',
  'dimension_id',
  'response_type',
  'response',
  'dimension_score',
  'experimental',
  'out_of_domain'
];

function setupPotentia() {
  const props = PropertiesService.getScriptProperties();
  const folder = DriveApp.getFolderById(TARGET_FOLDER_ID);

  let spreadsheetId = props.getProperty('SPREADSHEET_ID');
  let ss;

  if (spreadsheetId) {
    ss = SpreadsheetApp.openById(spreadsheetId);
  } else {
    ss = SpreadsheetApp.create(SPREADSHEET_NAME);
    spreadsheetId = ss.getId();
    DriveApp.getFileById(spreadsheetId).moveTo(folder);
    props.setProperty('SPREADSHEET_ID', spreadsheetId);
  }

  const sessionsSheet = ensureSheet_(ss, 'Sessions', SESSION_HEADERS);
  const responsesSheet = ensureSheet_(ss, 'Responses', RESPONSE_HEADERS);
  const qaSessionsSheet = ensureSheet_(ss, 'QA_Sessions', SESSION_HEADERS);
  const qaResponsesSheet = ensureSheet_(ss, 'QA_Responses', RESPONSE_HEADERS);
  ensureDashboard_(ss);
  ensureQaDashboard_(ss);

  let token = props.getProperty('UPLOAD_TOKEN');
  if (!token) {
    token = Utilities.getUuid().replace(/-/g, '') + Utilities.getUuid().replace(/-/g, '');
    props.setProperty('UPLOAD_TOKEN', token);
  }

  SpreadsheetApp.flush();
  updateDashboard_(ss, sessionsSheet, responsesSheet);
  updateQaDashboard_(ss, qaSessionsSheet, qaResponsesSheet);
  updateCsvBackup_(sessionsSheet, SESSION_CSV_BACKUP_NAME);
  updateCsvBackup_(responsesSheet, RESPONSE_CSV_BACKUP_NAME);

  console.log('Spreadsheet URL: ' + ss.getUrl());
  console.log('UPLOAD_TOKEN: ' + token);
  console.log('Target Drive folder: https://drive.google.com/drive/folders/' + TARGET_FOLDER_ID);
}

function doGet() {
  return json_({
    ok: true,
    service: 'POTENTIA pilot sync',
    schemaVersion: 'potentia-sync-v1'
  });
}

function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    const payload = JSON.parse((e && e.postData && e.postData.contents) || '{}');
    const props = PropertiesService.getScriptProperties();
    const expectedToken = props.getProperty('UPLOAD_TOKEN');

    if (!expectedToken) {
      return json_({ ok: false, error: 'Server is not initialized. Run setupPotentia().' });
    }

    if (!payload.uploadToken || payload.uploadToken !== expectedToken) {
      return json_({ ok: false, error: 'Unauthorized upload token.' });
    }

    if (payload.schemaVersion !== 'potentia-sync-v1') {
      return json_({ ok: false, error: 'Unsupported schemaVersion.' });
    }

    const session = payload.session;
    if (!session || !session.sessionId || !session.participantId) {
      return json_({ ok: false, error: 'Missing sessionId or participantId.' });
    }

    const spreadsheetId = props.getProperty('SPREADSHEET_ID');
    if (!spreadsheetId) {
      return json_({ ok: false, error: 'Spreadsheet is not initialized.' });
    }

    const ss = SpreadsheetApp.openById(spreadsheetId);
    const dataKind = payload.dataKind === 'qa_test' ? 'qa_test' : 'pilot';
    const isQa = dataKind === 'qa_test';
    const sessionsSheet = ensureSheet_(
      ss,
      isQa ? 'QA_Sessions' : 'Sessions',
      SESSION_HEADERS
    );
    const responsesSheet = ensureSheet_(
      ss,
      isQa ? 'QA_Responses' : 'Responses',
      RESPONSE_HEADERS
    );

    if (isQa) ensureQaDashboard_(ss);
    else ensureDashboard_(ss);

    if (sessionAlreadyExists_(sessionsSheet, session.sessionId)) {
      if (isQa) updateQaDashboard_(ss, sessionsSheet, responsesSheet);
      else updateDashboard_(ss, sessionsSheet, responsesSheet);
      return json_({
        ok: true,
        duplicate: true,
        dataKind: dataKind,
        sessionId: session.sessionId
      });
    }

    const receivedAt = new Date().toISOString();
    const dimensions = session.dimensions || {};
    const responses = Array.isArray(session.responses) ? session.responses : [];

    sessionsSheet.appendRow([
      receivedAt,
      session.participantId,
      session.sessionId,
      session.consentVersion || '',
      session.appVersion || '',
      session.startedAt || '',
      session.completedAt || '',
      session.assessmentVersion || '',
      session.scoringVersion || '',
      session.language || '',
      score_(dimensions, 'logical'),
      score_(dimensions, 'creative'),
      score_(dimensions, 'verbal'),
      score_(dimensions, 'spatial'),
      score_(dimensions, 'social'),
      score_(dimensions, 'practical'),
      responses.length
    ]);

    if (responses.length > 0) {
      const rows = responses.map(function (response) {
        const dim = dimensions[response.dimensionId] || {};
        return [
          receivedAt,
          session.participantId,
          session.sessionId,
          session.consentVersion || '',
          session.appVersion || '',
          session.startedAt || '',
          session.completedAt || '',
          session.assessmentVersion || '',
          session.scoringVersion || '',
          session.language || '',
          response.itemId || '',
          response.dimensionId || '',
          response.responseType || '',
          response.response == null ? '' : String(response.response),
          dim.score == null ? '' : dim.score,
          dim.experimental === true,
          dim.outOfDomain === true
        ];
      });

      responsesSheet
        .getRange(responsesSheet.getLastRow() + 1, 1, rows.length, RESPONSE_HEADERS.length)
        .setValues(rows);
    }

    SpreadsheetApp.flush();
    if (isQa) {
      updateQaDashboard_(ss, sessionsSheet, responsesSheet);
    } else {
      updateDashboard_(ss, sessionsSheet, responsesSheet);
      updateCsvBackup_(sessionsSheet, SESSION_CSV_BACKUP_NAME);
      updateCsvBackup_(responsesSheet, RESPONSE_CSV_BACKUP_NAME);
    }

    return json_({
      ok: true,
      duplicate: false,
      dataKind: dataKind,
      sessionId: session.sessionId,
      responseCount: responses.length
    });
  } catch (err) {
    console.error(err && err.stack ? err.stack : err);
    return json_({ ok: false, error: String(err) });
  } finally {
    lock.releaseLock();
  }
}

function ensureSheet_(ss, name, headers) {
  let sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
  }

  if (sheet.getLastRow() === 0) {
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
    sheet.setFrozenRows(1);
    sheet.getRange(1, 1, 1, headers.length).setFontWeight('bold');
  }

  return sheet;
}

function ensureDashboard_(ss) {
  let sheet = ss.getSheetByName('Dashboard');
  if (!sheet) {
    sheet = ss.insertSheet('Dashboard', 0);
  }
  sheet.setFrozenRows(2);
  return sheet;
}

function ensureQaDashboard_(ss) {
  let sheet = ss.getSheetByName('QA_Dashboard');
  if (!sheet) {
    sheet = ss.insertSheet('QA_Dashboard');
  }
  sheet.setFrozenRows(2);
  return sheet;
}

function updateDashboard_(ss, sessionsSheet, responsesSheet) {
  const dashboard = ensureDashboard_(ss);
  const sessionRows = sessionsSheet.getLastRow() > 1
    ? sessionsSheet.getRange(2, 1, sessionsSheet.getLastRow() - 1, SESSION_HEADERS.length).getDisplayValues()
    : [];
  const responseRows = responsesSheet.getLastRow() > 1
    ? responsesSheet.getRange(2, 1, responsesSheet.getLastRow() - 1, RESPONSE_HEADERS.length).getDisplayValues()
    : [];

  const participants = {};
  const appVersions = {};
  const assessmentVersions = {};
  const scoringVersions = {};
  let completeSessions = 0;
  let incompleteSessions = 0;
  let lastReceived = '';

  sessionRows.forEach(function (row) {
    if (row[1]) participants[row[1]] = true;
    if (row[4]) appVersions[row[4]] = true;
    if (row[7]) assessmentVersions[row[7]] = true;
    if (row[8]) scoringVersions[row[8]] = true;

    const responseCount = Number(row[16] || 0);
    if (responseCount >= 47) completeSessions += 1;
    else incompleteSessions += 1;

    if (row[0] && row[0] > lastReceived) lastReceived = row[0];
  });

  dashboard.clearContents();
  dashboard.getRange('A1').setValue('POTENTIA Pilot Monitor').setFontWeight('bold').setFontSize(16);
  dashboard.getRange('A2').setValue('Operational monitoring only — not psychometric validation or population norms.');

  const metrics = [
    ['Metric', 'Value'],
    ['Synced sessions', sessionRows.length],
    ['Unique pseudonymous participants', Object.keys(participants).length],
    ['Complete 47-response sessions', completeSessions],
    ['Sessions requiring review (<47 responses)', incompleteSessions],
    ['Total response rows', responseRows.length],
    ['Last received at', lastReceived || '—'],
    ['App versions', Object.keys(appVersions).sort().join(', ') || '—'],
    ['Assessment versions', Object.keys(assessmentVersions).sort().join(', ') || '—'],
    ['Scoring versions', Object.keys(scoringVersions).sort().join(', ') || '—']
  ];
  dashboard.getRange(4, 1, metrics.length, 2).setValues(metrics);
  dashboard.getRange(4, 1, 1, 2).setFontWeight('bold');

  dashboard.getRange('D4').setValue('Latest synced sessions').setFontWeight('bold');
  const latestHeaders = ['received_at', 'participant_id', 'session_id', 'app_version', 'response_count'];
  dashboard.getRange(5, 4, 1, latestHeaders.length).setValues([latestHeaders]).setFontWeight('bold');

  const latest = sessionRows
    .slice()
    .sort(function (a, b) { return String(b[0]).localeCompare(String(a[0])); })
    .slice(0, 20)
    .map(function (row) {
      return [row[0], row[1], row[2], row[4], row[16]];
    });

  if (latest.length > 0) {
    dashboard.getRange(6, 4, latest.length, latestHeaders.length).setValues(latest);
  }

  dashboard.autoResizeColumns(1, 8);
}

function updateQaDashboard_(ss, sessionsSheet, responsesSheet) {
  const dashboard = ensureQaDashboard_(ss);
  const sessionRows = sessionsSheet.getLastRow() > 1
    ? sessionsSheet.getRange(2, 1, sessionsSheet.getLastRow() - 1, SESSION_HEADERS.length).getDisplayValues()
    : [];
  const responseRows = responsesSheet.getLastRow() > 1
    ? responsesSheet.getRange(2, 1, responsesSheet.getLastRow() - 1, RESPONSE_HEADERS.length).getDisplayValues()
    : [];

  let lastReceived = '';
  sessionRows.forEach(function (row) {
    if (row[0] && row[0] > lastReceived) lastReceived = row[0];
  });

  dashboard.clearContents();
  dashboard.getRange('A1').setValue('POTENTIA Developer QA Monitor').setFontWeight('bold').setFontSize(16);
  dashboard.getRange('A2').setValue('Synthetic smoke-test data only. NEVER include these rows in pilot analysis.');

  const metrics = [
    ['Metric', 'Value'],
    ['QA test sessions', sessionRows.length],
    ['QA response rows', responseRows.length],
    ['Last QA received at', lastReceived || '—']
  ];
  dashboard.getRange(4, 1, metrics.length, 2).setValues(metrics);
  dashboard.getRange(4, 1, 1, 2).setFontWeight('bold');

  dashboard.getRange('D4').setValue('Latest QA smoke tests').setFontWeight('bold');
  const latestHeaders = ['received_at', 'session_id', 'app_version', 'response_count'];
  dashboard.getRange(5, 4, 1, latestHeaders.length).setValues([latestHeaders]).setFontWeight('bold');

  const latest = sessionRows
    .slice()
    .sort(function (a, b) { return String(b[0]).localeCompare(String(a[0])); })
    .slice(0, 20)
    .map(function (row) {
      return [row[0], row[2], row[4], row[16]];
    });

  if (latest.length > 0) {
    dashboard.getRange(6, 4, latest.length, latestHeaders.length).setValues(latest);
  }

  dashboard.autoResizeColumns(1, 8);
}

function sessionAlreadyExists_(sheet, sessionId) {
  if (sheet.getLastRow() < 2) return false;

  const range = sheet.getRange(2, 3, sheet.getLastRow() - 1, 1);
  const found = range
    .createTextFinder(String(sessionId))
    .matchEntireCell(true)
    .findNext();

  return found != null;
}

function score_(dimensions, id) {
  const dim = dimensions[id] || {};
  return dim.score == null ? '' : dim.score;
}

function updateCsvBackup_(sheet, fileName) {
  const folder = DriveApp.getFolderById(TARGET_FOLDER_ID);
  const values = sheet.getDataRange().getDisplayValues();
  const csv = values.map(function (row) {
    return row.map(csvCell_).join(',');
  }).join('\n') + '\n';

  const files = folder.getFilesByName(fileName);
  if (files.hasNext()) {
    files.next().setContent(csv);
    while (files.hasNext()) {
      files.next().setTrashed(true);
    }
  } else {
    folder.createFile(fileName, csv, MimeType.CSV);
  }
}

function csvCell_(value) {
  const text = value == null ? '' : String(value);
  return '"' + text.replace(/"/g, '""') + '"';
}

function json_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
