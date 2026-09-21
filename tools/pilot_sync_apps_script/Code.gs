const TARGET_FOLDER_ID = '1sfTUFFtuy0p9pqt5X8AaCV6BqTJe_2eF';
const SPREADSHEET_NAME = 'POTENTIA Pilot Live Data';
const CSV_BACKUP_NAME = 'potentia_pilot_responses_live.csv';

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

  ensureSheet_(ss, 'Sessions', SESSION_HEADERS);
  ensureSheet_(ss, 'Responses', RESPONSE_HEADERS);

  let token = props.getProperty('UPLOAD_TOKEN');
  if (!token) {
    token = Utilities.getUuid().replace(/-/g, '') + Utilities.getUuid().replace(/-/g, '');
    props.setProperty('UPLOAD_TOKEN', token);
  }

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
    const sessionsSheet = ensureSheet_(ss, 'Sessions', SESSION_HEADERS);
    const responsesSheet = ensureSheet_(ss, 'Responses', RESPONSE_HEADERS);

    if (sessionAlreadyExists_(sessionsSheet, session.sessionId)) {
      return json_({
        ok: true,
        duplicate: true,
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
    updateCsvBackup_(responsesSheet);

    return json_({
      ok: true,
      duplicate: false,
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
  }

  return sheet;
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

function updateCsvBackup_(responsesSheet) {
  const folder = DriveApp.getFolderById(TARGET_FOLDER_ID);
  const values = responsesSheet.getDataRange().getDisplayValues();
  const csv = values.map(function (row) {
    return row.map(csvCell_).join(',');
  }).join('\n') + '\n';

  const files = folder.getFilesByName(CSV_BACKUP_NAME);
  if (files.hasNext()) {
    files.next().setContent(csv);
    while (files.hasNext()) {
      files.next().setTrashed(true);
    }
  } else {
    folder.createFile(CSV_BACKUP_NAME, csv, MimeType.CSV);
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
