// Demo doc lap cho tinh nang "Tham gia lop hoc" (FD-08 / flow F4). Tach rieng
// khoi tests/e2e/validation-rules-mutating.spec.js de mo va chay truc tiep
// trong buoi phong van, khong lan sang 5 form khac trong file goc.
//
// 2 tang:
//   Tang 1 (component)  - 8 test case kiem tra tung truong du lieu ma moi,
//                          sao chep nguyen ban tu describe('FD-08 - Tham gia
//                          lop bang ma moi') trong file goc (van con dung).
//   Tang 2 (E2E hanh trinh) - 1 kich ban moi, bam sat flow F4.V1 trong
//                          docs/system-test-flows.md: dang ky -> dang nhap ->
//                          nhap ma tham gia -> PENDING -> GV duyet -> ACTIVE
//                          -> SV mo duoc roadmap va thay node. Day la phan
//                          truoc day chua ton tai - FD-08 chi test tung
//                          truong rieng le, chua co kich ban di het hanh
//                          trinh nguoi dung that.
//
// Ghi chu: sheet FD-08 trong Mentora-TestCase-6Forms.xlsx co them 1 dong BVA
// (ma moi 9 ky tu - qua dai) chua duoc dong bo vao Playwright (xem comment
// dau file validation-rules-mutating.spec.js) - dong bo da CHUA duoc lam o
// day, giu nguyen 8 TC nhu ban goc.
//
// Chay:  RUN_VALIDATION_E2E=1 npx playwright test tests/e2e/join-classroom.spec.js
// hoac:  npm run test:e2e:join-classroom
const { test, expect } = require('@playwright/test');
const { config, login, registerStudent } = require('./helpers/mentora');

const runValidation = process.env.RUN_VALIDATION_E2E === '1';

function marker() {
  return `${Date.now()}-${Math.floor(Math.random() * 100000)}`;
}

/** Tat HTML5 constraint validation cua form tren trang, de cac case "de
 * trong"/"sai dinh dang" thuc su toi duoc server thay vi bi trinh duyet chan. */
async function disableNativeValidation(page) {
  await page.evaluate(() => {
    document.querySelectorAll('form').forEach((f) => { f.noValidate = true; });
  });
}

async function expectAlertDanger(page, message) {
  await expect(page.locator('.alert-danger')).toHaveText(message);
}

async function expectAlertSuccessContains(page, snippet) {
  await expect(page.locator('.alert-success')).toContainText(snippet);
}

async function submitJoin(page, code) {
  await page.goto('/student/classrooms', { waitUntil: 'domcontentloaded' });
  await disableNativeValidation(page);
  await page.locator('#inviteCode').fill(code);
  await page.locator('form[action$="/join"] button[type="submit"]').click();
}

/** Liet ke moi cap (subjectValue, pathValue) ma giang vien dang dang nhap
 * so huu, bang cach chon lan luot tung Mon hoc va doc <select
 * id="learningPathId"> (bi loc bang JS theo mon da chon). */
async function listOwnedSubjectPathPairs(page) {
  const subjectValues = await page.locator('#subjectId option:not([value=""])').evaluateAll(
    (opts) => opts.map((o) => o.value)
  );
  const pairs = [];
  for (const subjectValue of subjectValues) {
    await page.locator('#subjectId').selectOption(subjectValue);
    const pathValues = await page.locator('#learningPathId option:not([value=""])').evaluateAll(
      (opts) => opts.map((o) => o.value)
    );
    for (const pathValue of pathValues) {
      pairs.push({ subjectValue, pathValue });
    }
  }
  return pairs;
}

// Cache cap (mon hoc, lo trinh) da xac nhan tao lop thanh cong - thuoc tinh
// co dinh cua tai khoan giang vien seed, khong doi giua cac test trong file.
let cachedWorkingPair = null;

/** Tim 1 cap (mon hoc, lo trinh) chac chan tao lop thanh cong duoc (co node),
 * bang cach thu tao that 1 lop throwaway cho tung cap cho toi khi thanh cong. */
async function getWorkingSubjectPathPair(lecturerPage) {
  if (cachedWorkingPair) return cachedWorkingPair;

  await lecturerPage.goto('/lecturer/classes/new', { waitUntil: 'domcontentloaded' });
  const pairs = await listOwnedSubjectPathPairs(lecturerPage);

  for (const pair of pairs) {
    await lecturerPage.locator('input[name="name"]').fill(`E2E Probe ${marker()}`);
    await lecturerPage.locator('#subjectId').selectOption(pair.subjectValue);
    await lecturerPage.locator('#learningPathId').selectOption(pair.pathValue);
    const semesterValue = await lecturerPage.locator('select[name="semesterId"] option:not([value=""])').first().getAttribute('value');
    await lecturerPage.locator('select[name="semesterId"]').selectOption(semesterValue);
    await lecturerPage.locator('button[type="submit"]').click();

    if (/\/lecturer\/classes$/.test(new URL(lecturerPage.url()).pathname)) {
      cachedWorkingPair = pair;
      return pair;
    }
    await lecturerPage.goto('/lecturer/classes/new', { waitUntil: 'domcontentloaded' });
  }
  throw new Error('Khong tim thay cap (mon hoc, lo trinh) nao tao lop thanh cong duoc.');
}

/** Tao 1 lop hoc that qua UI giang vien, tra ve { name, inviteCode }. */
async function createClassroomFixture(lecturerPage, status) {
  const id = marker();
  const name = `E2E Lop ${status} ${id}`;
  const pair = await getWorkingSubjectPathPair(lecturerPage);

  await lecturerPage.goto('/lecturer/classes/new', { waitUntil: 'domcontentloaded' });
  await lecturerPage.locator('input[name="name"]').fill(name);
  await lecturerPage.locator('#subjectId').selectOption(pair.subjectValue);
  await lecturerPage.locator('#learningPathId').selectOption(pair.pathValue);
  const semesterValue = await lecturerPage.locator('select[name="semesterId"] option:not([value=""])').first().getAttribute('value');
  await lecturerPage.locator('select[name="semesterId"]').selectOption(semesterValue);
  await lecturerPage.locator('select[name="status"]').selectOption(status);
  await lecturerPage.locator('button[type="submit"]').click();

  await expect(lecturerPage).toHaveURL(/\/lecturer\/classes$/);
  const alertText = await lecturerPage.locator('.alert-success').textContent();
  const match = alertText.match(/Mã mời:\s*([A-Z0-9]{8})/);
  if (!match) throw new Error(`Khong doc duoc ma moi tu: ${alertText}`);
  return { name, inviteCode: match[1] };
}

// =====================================================================
// Tang 1 - FD-08 - Tham gia lop bang ma moi (component, 8 test case)
// =====================================================================
test.describe('Tang 1 - FD-08 kiem tra truong ma moi', () => {
  test.skip(!runValidation, 'Set RUN_VALIDATION_E2E=1 to run.');

  // Lam nong cachedWorkingPair truoc, ngoai ngan sach timeout 45s cua tung
  // test - neu de TC-05 (test dau tien can classroom fixture) tu do cap
  // (mon hoc, lo trinh) qua nhieu lan submit that, no an het timeout va
  // lam login ke tiep bi cat giua chung.
  test.beforeAll(async ({ browser }) => {
    if (!runValidation) return;
    test.setTimeout(120_000); // do that qua nhieu cap thu that co the gan 45s mac dinh
    const page = await browser.newPage();
    await login(page, config.lecturer);
    await getWorkingSubjectPathPair(page);
    await page.close();
  });

  test('TC-01: ma lop de trong', async ({ page }) => {
    await login(page, config.student);
    await submitJoin(page, '');
    await expect(page).toHaveURL(/\/student\/classrooms$/);
    await expectAlertDanger(page, 'Vui lòng nhập mã lớp.');
  });

  test('TC-02: ma lop chi co 7 ky tu', async ({ page }) => {
    await login(page, config.student);
    await submitJoin(page, 'ST2026O');
    await expectAlertDanger(page, 'Mã lớp không hợp lệ.');
  });

  test('TC-03: ma lop chua ky tu bi cam (chu I)', async ({ page }) => {
    await login(page, config.student);
    await submitJoin(page, 'ST2I26OK');
    await expectAlertDanger(page, 'Mã lớp không hợp lệ.');
  });

  test('TC-04: ma lop dung 8 ky tu nhung khong khop lop nao', async ({ page }) => {
    await login(page, config.student);
    await submitJoin(page, 'ZZZZZZZZ');
    await expectAlertDanger(page, 'Mã lớp không hợp lệ.');
  });

  test('TC-05: ma lop khop mot lop da dong', async ({ browser }) => {
    const lecturerPage = await browser.newPage();
    await login(lecturerPage, config.lecturer);
    const closedClass = await createClassroomFixture(lecturerPage, 'CLOSE');
    await lecturerPage.close();

    const studentPage = await browser.newPage();
    await login(studentPage, config.student);
    await submitJoin(studentPage, closedClass.inviteCode);
    await expectAlertDanger(studentPage, 'Lớp học này đã đóng, không thể tham gia.');
    await studentPage.close();
  });

  test('TC-06: hoc sinh nhap lai ma lop da tham gia truoc do', async ({ browser }) => {
    const lecturerPage = await browser.newPage();
    await login(lecturerPage, config.lecturer);
    const openClass = await createClassroomFixture(lecturerPage, 'OPEN');
    await lecturerPage.close();

    const id = marker();
    const student = { fullName: 'Test Join Student', email: `e2e.join.${id}@mentora.test`, password: 'Abc@1234' };
    const studentPage = await browser.newPage();
    await registerStudent(studentPage, student);
    await login(studentPage, student);

    await submitJoin(studentPage, openClass.inviteCode);
    await expectAlertSuccessContains(studentPage, 'Đã gửi yêu cầu tham gia lớp');

    await submitJoin(studentPage, openClass.inviteCode);
    await expectAlertDanger(studentPage, 'Bạn đã gửi yêu cầu hoặc đã là thành viên lớp này rồi.');
    await studentPage.close();
  });

  test('TC-07: nhap ma lop bang chu thuong - thanh cong', async ({ browser }) => {
    const lecturerPage = await browser.newPage();
    await login(lecturerPage, config.lecturer);
    const openClass = await createClassroomFixture(lecturerPage, 'OPEN');
    await lecturerPage.close();

    const id = marker();
    const student = { fullName: 'Test Lower Student', email: `e2e.lower.${id}@mentora.test`, password: 'Abc@1234' };
    const studentPage = await browser.newPage();
    await registerStudent(studentPage, student);
    await login(studentPage, student);

    await submitJoin(studentPage, openClass.inviteCode.toLowerCase());
    await expectAlertSuccessContains(studentPage, 'Đã gửi yêu cầu tham gia lớp');
    await studentPage.close();
  });

  test('TC-08: ma lop co khoang trang thua o dau va cuoi - thanh cong', async ({ browser }) => {
    const lecturerPage = await browser.newPage();
    await login(lecturerPage, config.lecturer);
    const openClass = await createClassroomFixture(lecturerPage, 'OPEN');
    await lecturerPage.close();

    const id = marker();
    const student = { fullName: 'Test Trim Student', email: `e2e.trim.${id}@mentora.test`, password: 'Abc@1234' };
    const studentPage = await browser.newPage();
    await registerStudent(studentPage, student);
    await login(studentPage, student);

    await submitJoin(studentPage, `  ${openClass.inviteCode}  `);
    await expectAlertSuccessContains(studentPage, 'Đã gửi yêu cầu tham gia lớp');
    await studentPage.close();
  });
});

// =====================================================================
// Tang 2 - F4.V1 - Hanh trinh day du: dang ky > tham gia > duyet > vao hoc
// (docs/system-test-flows.md, F4 stage map S0 -> S1 -> S2 -> S3)
// =====================================================================
test.describe('Tang 2 - F4.V1 hanh trinh gia nhap lop hoc day du', () => {
  test.skip(!runValidation, 'Set RUN_VALIDATION_E2E=1 to run.');

  test('V1: dang ky -> tham gia lop -> GV duyet -> vao hoc duoc roadmap', async ({ browser }) => {
    test.setTimeout(90_000); // hanh trinh nhieu buoc, qua 2 phien GV + SV
    // GV: dung lop OPEN that (co node) lam fixture cho hanh trinh
    const lecturerPage = await browser.newPage();
    await login(lecturerPage, config.lecturer);
    const openClass = await createClassroomFixture(lecturerPage, 'OPEN');

    // S0 (AU-03): SV moi dang ky -> dang nhap
    const id = marker();
    const student = { fullName: 'Test Journey Student', email: `e2e.journey.${id}@mentora.test`, password: 'Abc@1234' };
    const studentPage = await browser.newPage();
    await registerStudent(studentPage, student);
    await login(studentPage, student);
    await expect(studentPage).toHaveURL(/\/student\/dashboard/);

    // S1 (JOIN-01): SV nhap ma moi -> gui yeu cau, trang thai PENDING
    await submitJoin(studentPage, openClass.inviteCode);
    await expectAlertSuccessContains(studentPage, 'Đã gửi yêu cầu tham gia lớp. Chờ giảng viên chấp nhận.');
    await expect(
      studentPage.locator('.student-class-card', { hasText: openClass.name }).locator('.student-pending-badge')
    ).toContainText('Chờ duyệt');

    // S2 (JOIN-01): GV mo trang thanh vien cua chinh lop vua tao -> Chap nhan
    await lecturerPage.goto('/lecturer/classes', { waitUntil: 'domcontentloaded' });
    const membersHref = await lecturerPage
      .locator('.lecturer-class-card', { hasText: openClass.name })
      .locator('a[href*="/members"]')
      .first()
      .getAttribute('href');
    await lecturerPage.goto(membersHref, { waitUntil: 'domcontentloaded' });
    const pendingRow = lecturerPage.locator('tr', { hasText: student.email });
    await expect(pendingRow).toBeVisible();
    await pendingRow.locator('form[action$="/approve"] button[type="submit"]').click();
    await expectAlertSuccessContains(lecturerPage, 'Đã chấp nhận yêu cầu tham gia.');

    // Node moi tao mac dinh HIDDEN o cap lop hoc - GV phai bat hien thi it
    // nhat 1 node thi SV moi thay duoc gi tren roadmap (khac voi "co node
    // trong lo trinh", day la buoc rieng biet o /lecturer/classes/{id}/nodes).
    const classroomIdMatch = membersHref.match(/\/lecturer\/classes\/(\d+)\/members/);
    const classroomId = classroomIdMatch[1];
    await lecturerPage.goto(`/lecturer/classes/${classroomId}/nodes`, { waitUntil: 'domcontentloaded' });
    await lecturerPage.locator('form[action$="/show"] button[type="submit"]').first().click();
    await expectAlertSuccessContains(lecturerPage, 'Đã bật hiển thị node cho lớp.');

    // S3 (JOIN-01, LT-19): SV quay lai danh sach lop, thanh vien da ACTIVE,
    // mo duoc roadmap va thay node cua lop
    await studentPage.goto('/student/classrooms', { waitUntil: 'domcontentloaded' });
    const classCard = studentPage.locator('.student-class-card', { hasText: openClass.name });
    await expect(classCard).toBeVisible();
    await classCard.locator('a', { hasText: 'Xem lộ trình' }).click();
    await expect(studentPage).toHaveURL(/\/student\/classrooms\/\d+\/roadmap$/);
    await expect(studentPage.locator('.roadmap-node').first()).toBeVisible();

    await lecturerPage.close();
    await studentPage.close();
  });
});
