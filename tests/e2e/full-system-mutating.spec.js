const { test, expect } = require('@playwright/test');
const {
  config,
  login,
  logout,
  registerStudent,
  expectNoServerError,
  gotoAndExpectOk,
  requiredAttribute
} = require('./helpers/mentora');

const runFull = process.env.RUN_FULL_E2E === '1';
const marker = `${Date.now()}-${Math.floor(Math.random() * 1000)}`;
const createdStudent = {
  fullName: `E2E Student ${marker}`,
  email: `e2e.student.${marker}@mentora.test`,
  password: 'password'
};
const createdTeacherEmail = `e2e.teacher.${marker}@mentora.test`;
const createdSubjectCode = `E2E${marker.slice(-6)}`.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 10);
const createdSemesterName = `E2E Semester ${marker}`;
const semesterYear = 2100 + (Number(marker.replace(/\D/g, '').slice(-4)) % 7000);
const semesterStartDate = `${semesterYear}-01-01`;
const semesterEndDate = `${semesterYear}-04-30`;

let levelTakePath;

test.describe.serial('Mentora full system mutating coverage', () => {
  test.skip(!runFull, 'Set RUN_FULL_E2E=1 to run broad database-mutating system checks.');

  test('admin can create teacher, semester, and subject records', async ({ page }) => {
    await login(page, config.admin);

    await gotoAndExpectOk(page, '/admin/accounts?tab=lecturers');
    await page.locator('[data-bs-target="#createTeacherModal"]').click();
    await page.locator('#ct-fullName').fill(`E2E Teacher ${marker}`);
    await page.locator('#ct-email').fill(createdTeacherEmail);
    await page.locator('#ct-password').fill('password');
    await page.locator('#createTeacherModal form button[type="submit"]').click();
    await expect(page).toHaveURL(/\/admin\/accounts/);
    await gotoAndExpectOk(page, `/admin/accounts?tab=lecturers&search=${encodeURIComponent(createdTeacherEmail)}`);
    await expect(page.locator('body')).toContainText(createdTeacherEmail);

    await gotoAndExpectOk(page, '/admin/semesters');
    await page.locator('[data-bs-target="#createSemesterModal"]').click();
    await page.locator('#cs-name').fill(createdSemesterName);
    await page.locator('#cs-startDate').evaluate((el, value) => { el.value = value; }, semesterStartDate);
    await page.locator('#cs-endDate').evaluate((el, value) => { el.value = value; }, semesterEndDate);
    await page.locator('#cs-status').selectOption('HIDDEN');
    await page.locator('#createSemesterModal form button[type="submit"]').click();
    await expect(page.locator('body')).toContainText(createdSemesterName);

    await gotoAndExpectOk(page, '/admin/subjects?create=true');
    await page.locator('#code').fill(createdSubjectCode);
    await page.locator('#name').fill(`E2E Subject ${marker}`);
    await page.locator('#description').fill('Created by Playwright full-system coverage.');
    await page.locator('#status').selectOption('HIDDEN');
    await page.locator('#subjectModal form button[type="submit"]').click();
    await expect(page.locator('body')).toContainText(createdSubjectCode);
  });

  test('student can register, request class access, and lecturer can approve and toggle TA role', async ({ browser }) => {
    const studentPage = await browser.newPage();
    await registerStudent(studentPage, createdStudent);
    await login(studentPage, createdStudent);
    await gotoAndExpectOk(studentPage, '/student/classrooms');
    await studentPage.locator('#inviteCode').fill(config.inviteCode);
    await studentPage.locator('form[action$="/join"] button[type="submit"]').click();
    await expectNoServerError(studentPage);
    await expect(studentPage.locator('body')).toContainText(/SE1842|SWP391|Đang chờ|Chá» duyá»‡t/i);
    await studentPage.close();

    const lecturerPage = await browser.newPage();
    await login(lecturerPage, config.lecturer);
    await gotoAndExpectOk(lecturerPage, `/lecturer/classes/${config.classroomId}/members`);

    const pendingRow = lecturerPage.locator('tr', { hasText: createdStudent.email });
    await expect(pendingRow).toBeVisible();
    await pendingRow.locator('form[action*="/approve"] button[type="submit"]').click();
    await expectNoServerError(lecturerPage);

    const activeRow = lecturerPage.locator('tr', { hasText: createdStudent.email });
    await expect(activeRow).toBeVisible();
    await activeRow.locator('form[action*="/assign-ta"] button[type="submit"]').click();
    await expectNoServerError(lecturerPage);
    await expect(lecturerPage.locator('tr', { hasText: createdStudent.email }).locator('form[action*="/revoke-ta"]')).toBeVisible();

    await lecturerPage.locator('tr', { hasText: createdStudent.email }).locator('form[action*="/revoke-ta"] button[type="submit"]').click();
    await expectNoServerError(lecturerPage);
    await expect(lecturerPage.locator('tr', { hasText: createdStudent.email }).locator('form[action*="/assign-ta"]')).toBeVisible();
    await lecturerPage.close();
  });

  test('approved student can review material, submit a level test, and see result review', async ({ page }) => {
    await login(page, createdStudent);
    await gotoAndExpectOk(page, `/student/classrooms/${config.classroomId}/nodes/${config.studentNodeId}`);

    await page.locator('#levelAccordion .accordion-button').first().click();
    const reviewButton = page.locator('form[action*="/reviewed"] button[type="submit"]').first();
    if (await reviewButton.isVisible()) {
      await reviewButton.click();
      await expectNoServerError(page);
    }

    const takeLink = page.locator('a[href*="/student/classrooms/"][href*="/levels/"][href$="/take"]').first();
    await expect(takeLink).toBeVisible();
    levelTakePath = await requiredAttribute(takeLink, 'href');
    await gotoAndExpectOk(page, levelTakePath);

    await expect(page.locator('#levelTestForm')).toBeVisible();
    const questionCards = await page.locator('.student-question-card').all();
    expect(questionCards.length).toBeGreaterThan(0);
    for (const card of questionCards) {
      await card.locator('input[type="radio"]').first().check();
    }

    await page.locator('#submitTestButton').click();
    await page.locator('#examActionConfirm').click();
    await expect(page).toHaveURL(/\/take\/result\?attemptId=\d+/);
    await expectNoServerError(page);
    await expect(page.locator('.student-result-card')).toBeVisible();
    await expect(page.locator('.student-review-question').first()).toBeVisible();
  });

  test('lecturer can grant attempt, hide/show node, and complete class blocks new attempts until reopen', async ({ browser }) => {
    const lecturerPage = await browser.newPage();
    const studentPage = await browser.newPage();

    await login(lecturerPage, config.lecturer);
    await gotoAndExpectOk(lecturerPage, `/lecturer/classes/${config.classroomId}/results`);
    const studentRow = lecturerPage.locator('tr[data-href]', { hasText: createdStudent.email });
    const studentDetailHref = await requiredAttribute(studentRow, 'data-href');
    await gotoAndExpectOk(lecturerPage, studentDetailHref);
    await lecturerPage.locator('[data-bs-target="#grantAttemptModal"]').first().click();
    await lecturerPage.locator('#confirmGrantAttempt').click();
    await expectNoServerError(lecturerPage);
    await expect(lecturerPage.locator('body')).toContainText(/\+1|Lượt|lÆ°á»£t/i);

    await gotoAndExpectOk(lecturerPage, `/lecturer/classes/${config.classroomId}/nodes`);
    const nodeRow = lecturerPage.locator('tr', { hasText: 'Sprint 0' });
    await expect(nodeRow).toBeVisible();
    await nodeRow.locator('form[action$="/hide"] button[type="submit"]').click();
    await expectNoServerError(lecturerPage);
    await expect(lecturerPage.locator('tr', { hasText: 'Sprint 0' }).locator('form[action$="/show"]')).toBeVisible();
    await lecturerPage.locator('tr', { hasText: 'Sprint 0' }).locator('form[action$="/show"] button[type="submit"]').click();
    await expectNoServerError(lecturerPage);
    await expect(lecturerPage.locator('tr', { hasText: 'Sprint 0' }).locator('form[action$="/hide"]')).toBeVisible();

    await gotoAndExpectOk(lecturerPage, `/lecturer/classes/${config.classroomId}/results`);
    try {
      lecturerPage.once('dialog', (dialog) => dialog.accept());
      await lecturerPage.locator('form[action$="/complete"] button[type="submit"]').click();
      await expectNoServerError(lecturerPage);
      await expect(lecturerPage.locator('form[action$="/reopen"] button[type="submit"]')).toBeVisible();

      await login(studentPage, createdStudent);
      await gotoAndExpectOk(studentPage, levelTakePath);
      await expect(studentPage).not.toHaveURL(/\/take$/);
      await expect(studentPage.locator('body')).toContainText(/kết thúc|káº¿t thÃºc|không thể|khÃ´ng thá»ƒ|lớp/i);
    } finally {
      await login(lecturerPage, config.lecturer);
      await gotoAndExpectOk(lecturerPage, `/lecturer/classes/${config.classroomId}/results`);
      if (await lecturerPage.locator('form[action$="/reopen"] button[type="submit"]').isVisible()) {
        lecturerPage.once('dialog', (dialog) => dialog.accept());
        await lecturerPage.locator('form[action$="/reopen"] button[type="submit"]').click();
        await expectNoServerError(lecturerPage);
      }
      await lecturerPage.close();
      await studentPage.close();
    }
  });
});
