const { expect } = require('@playwright/test');

const config = {
  admin: {
    email: process.env.E2E_ADMIN_EMAIL || 'admin1@mentora.com',
    password: process.env.E2E_ADMIN_PASSWORD || 'admin123'
  },
  lecturer: {
    email: process.env.E2E_LECTURER_EMAIL || 'giangvien.swp391@mentora.edu.vn',
    password: process.env.E2E_LECTURER_PASSWORD || 'password'
  },
  student: {
    email: process.env.E2E_STUDENT_EMAIL || 'sinhvien01@mentora.edu.vn',
    password: process.env.E2E_STUDENT_PASSWORD || 'password'
  },
  secondStudent: {
    email: process.env.E2E_SECOND_STUDENT_EMAIL || 'sinhvien02@mentora.edu.vn',
    password: process.env.E2E_SECOND_STUDENT_PASSWORD || 'password'
  },
  classroomId: process.env.E2E_CLASSROOM_ID || '10',
  secondClassroomId: process.env.E2E_SECOND_CLASSROOM_ID || '11',
  inviteCode: process.env.E2E_JOIN_CODE || 'SWP1842',
  materialSubjectId: process.env.E2E_MATERIAL_SUBJECT_ID || '13',
  snapshotAttemptId: process.env.E2E_SNAPSHOT_ATTEMPT_ID || '293',
  studentNodeId: process.env.E2E_STUDENT_NODE_ID || '45'
};

async function login(page, account) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' });
  if (/\/(admin|lecturer|student)\/dashboard/.test(new URL(page.url()).pathname)) {
    await page.goto('/logout', { waitUntil: 'domcontentloaded' });
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
  }
  await page.locator('#email').fill(account.email);
  await page.locator('#password').fill(account.password);
  await page.locator('form[action$="/login"] button[type="submit"]').click();
  await expect(page).toHaveURL(/\/(admin|lecturer|student)\/dashboard/);
}

async function registerStudent(page, account) {
  await page.goto('/register', { waitUntil: 'domcontentloaded' });
  await page.locator('#fullName').fill(account.fullName);
  await page.locator('#email').fill(account.email);
  await page.locator('#password').fill(account.password);
  await page.locator('form[action$="/register"] button[type="submit"]').click();
  await expect(page).toHaveURL(/\/login/);
}

async function logout(page) {
  await page.goto('/logout', { waitUntil: 'domcontentloaded' });
  await expect(page).toHaveURL(/\/login/);
}

async function expectNoServerError(page) {
  await expect(page.locator('body')).not.toContainText(
    /Whitelabel Error Page|HTTP Status 500|This application has no explicit mapping for \/error|There was an unexpected error/i
  );
}

async function gotoAndExpectOk(page, url) {
  const response = await page.goto(url, { waitUntil: 'domcontentloaded' });
  expect(response, `GET ${url}`).not.toBeNull();
  expect(response.status(), `GET ${url}`).toBeLessThan(500);
  await expectNoServerError(page);
  return response;
}

async function requiredAttribute(locator, attributeName) {
  const value = await locator.getAttribute(attributeName);
  expect(value, `${attributeName} must be present`).toBeTruthy();
  return value;
}

module.exports = {
  config,
  login,
  registerStudent,
  logout,
  expectNoServerError,
  gotoAndExpectOk,
  requiredAttribute
};
