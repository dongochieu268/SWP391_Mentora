const { test, expect } = require('@playwright/test');
const { config, login, logout, expectNoServerError, gotoAndExpectOk, requiredAttribute } = require('./helpers/mentora');

test.describe('Mentora full dump readonly regression smoke', () => {
  test('routes each seeded role to the correct dashboard', async ({ page }) => {
    await login(page, config.admin);
    await expect(page).toHaveURL(/\/admin\/dashboard/);
    await expectNoServerError(page);

    await logout(page);
    await login(page, config.lecturer);
    await expect(page).toHaveURL(/\/lecturer\/dashboard/);
    await expectNoServerError(page);

    await logout(page);
    await login(page, config.student);
    await expect(page).toHaveURL(/\/student\/dashboard/);
    await expectNoServerError(page);
  });

  test('lecturer can read imported results, student detail, attempt snapshot, and CSV', async ({ page }) => {
    await login(page, config.lecturer);
    await gotoAndExpectOk(page, `/lecturer/classes/${config.classroomId}/results`);

    await expect(page.locator('tr[data-href]').first()).toBeVisible();
    const csvLink = page.locator('a[href$="/results/export.csv"]');
    await expect(csvLink).toBeVisible();

    const studentRow = page.locator('tr[data-href]', { hasText: 'sinhvien05@mentora.edu.vn' });
    const studentDetailHref = await requiredAttribute(studentRow, 'data-href');
    await gotoAndExpectOk(page, studentDetailHref);
    await expectNoServerError(page);
    await expect(page).toHaveURL(new RegExp(`/lecturer/classes/${config.classroomId}/results/students/\\d+`));

    const submittedAttempt = page.locator('a[href*="/results/attempts/"]').first();
    await expect(submittedAttempt).toBeVisible();
    await gotoAndExpectOk(page, `/lecturer/classes/${config.classroomId}/results/attempts/${config.snapshotAttemptId}`);
    await expectNoServerError(page);
    await expect(page).toHaveURL(new RegExp(`/lecturer/classes/${config.classroomId}/results/attempts/\\d+`));
    await expect(page.locator('.list-group-item').first()).toBeVisible();

    await gotoAndExpectOk(page, `/lecturer/classes/${config.classroomId}/results`);
    const downloadPromise = page.waitForEvent('download');
    await csvLink.click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toMatch(/ket-qua-lop-.*\.csv/);
  });

  test('student can read imported roadmap and open an accessible learning node', async ({ page }) => {
    await login(page, config.student);
    await gotoAndExpectOk(page, `/student/classrooms/${config.classroomId}/roadmap`);

    await expect(page.locator('body')).toContainText(/SWP391|SE1842/);
    await expect(page.locator(`a[href*="/student/classrooms/${config.classroomId}/nodes/"]`).first()).toBeVisible();

    await gotoAndExpectOk(page, `/student/classrooms/${config.classroomId}/nodes/${config.studentNodeId}`);
    await expectNoServerError(page);
    await expect(page).toHaveURL(new RegExp(`/student/classrooms/${config.classroomId}/nodes/\\d+`));
    await expect(page.locator('.student-lesson-card')).toBeVisible();
    const nodeBodySections = page.locator('#levelAccordion, #btnComplete, .learning-text-block, .learning-resource');
    expect(await nodeBodySections.count()).toBeGreaterThan(0);
  });

  test('lecturer material library opens on imported SWP391 subject', async ({ page }) => {
    await login(page, config.lecturer);
    await gotoAndExpectOk(page, `/lecturer/materials?subjectId=${config.materialSubjectId}`);

    await expect(page.locator('#subjectFilter')).toHaveValue(config.materialSubjectId);
    await expect(page.locator('.lecturer-material-item').first()).toBeVisible();
    await expect(page.locator('body')).toContainText(/SWP391|Material/);
  });

  test('QnA pages render imported answered and open questions for lecturer and student', async ({ page }) => {
    await login(page, config.lecturer);
    await gotoAndExpectOk(page, `/lecturer/classes/${config.classroomId}/qna`);
    await expect(page.locator('.question-row[data-status="OPEN"]').first()).toBeVisible();
    await expect(page.locator('.question-row[data-status="ANSWERED"]').first()).toBeVisible();

    await logout(page);
    await login(page, config.student);
    await gotoAndExpectOk(page, `/student/classrooms/${config.classroomId}/qna`);
    await expect(page.locator('.question-row').first()).toBeVisible();
    await expect(page.locator('#studentQuestion')).toBeVisible();
  });
});
