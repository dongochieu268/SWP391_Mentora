const { test, expect } = require('@playwright/test');
const { config, login, expectNoServerError, gotoAndExpectOk } = require('./helpers/mentora');

const runMutating = process.env.RUN_MUTATING_E2E === '1';

test.describe('Mentora full dump mutating regression flows', () => {
  test.skip(!runMutating, 'Set RUN_MUTATING_E2E=1 to run database-mutating checks.');
  test.describe.configure({ mode: 'serial' });

  test('student can create a QnA question and lecturer can answer it', async ({ browser }) => {
    const marker = `E2E QnA ${Date.now()}`;
    const studentPage = await browser.newPage();

    await login(studentPage, config.secondStudent);
    await gotoAndExpectOk(studentPage, `/student/classrooms/${config.classroomId}/qna`);
    await studentPage.locator('#studentQuestion').fill(marker);
    await studentPage.locator('form[action$="/questions"] button[type="submit"]').click();
    await expectNoServerError(studentPage);
    await expect(studentPage.locator('.question-row', { hasText: marker })).toBeVisible();
    await studentPage.close();

    const lecturerPage = await browser.newPage();
    await login(lecturerPage, config.lecturer);
    await gotoAndExpectOk(lecturerPage, `/lecturer/classes/${config.classroomId}/qna`);

    const row = lecturerPage.locator('.question-row', { hasText: marker });
    await expect(row).toBeVisible();
    await row.locator('[data-feedback-toggle]').click();
    await row.locator('textarea[name="answerContent"]').fill(`Answer for ${marker}`);
    await row.locator('form[action*="/answer"] button[type="submit"]').click();
    await expectNoServerError(lecturerPage);
    await expect(lecturerPage.locator('.question-row', { hasText: marker })).toHaveAttribute('data-status', 'ANSWERED');
    await lecturerPage.close();
  });

  test('lecturer can complete and reopen a class', async ({ page }) => {
    await login(page, config.lecturer);
    await gotoAndExpectOk(page, `/lecturer/classes/${config.secondClassroomId}/results`);

    page.once('dialog', (dialog) => dialog.accept());
    await page.locator('form[action$="/complete"] button[type="submit"]').click();
    await expectNoServerError(page);
    await expect(page.locator('form[action$="/reopen"] button[type="submit"]')).toBeVisible();

    page.once('dialog', (dialog) => dialog.accept());
    await page.locator('form[action$="/reopen"] button[type="submit"]').click();
    await expectNoServerError(page);
    await expect(page.locator('form[action$="/complete"] button[type="submit"]')).toBeVisible();
  });
});
