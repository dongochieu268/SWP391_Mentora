const {test, expect} = require('@playwright/test');

const BASE = {
    classroom: 'Lớp System Test',
    inviteCode: 'ST2026OK',
    node1: 'ST Node 1',
    node2: 'ST Node 2'
};

const lecturer = {
    email: 'st.gv1@mentora.test',
    password: 'password'
};

function newStudent() {
    const runId = `${Date.now().toString().slice(-8)}${Math.floor(Math.random() * 100)}`;
    return {
        fullname: `SV ${runId}`,
        email: `sv.${runId}@mentora.test`,
        password: 'password'
    };
}

async function login(page, account) {
    await page.goto('/login');
    await page.locator('#email').fill(account.email);
    await page.locator('#password').fill(account.password);
    await page.locator('form[action$="/login"] button[type="submit"]').click();
    await expect(page).toHaveURL(/\/(admin|lecturer|student)\/dashboard/);
}

async function register(page, account) {
    await page.goto('/register');
    await page.locator('#fullName').fill(account.fullname);
    await page.locator('#email').fill(account.email);
    await page.locator('#password').fill(account.password);
    await page.locator('form[action$="/register"] button[type="submit"]').click();
    await expect(page).toHaveURL(/\/login/);
}

async function findClassroomIdAsLecturer(page) {
    await page.goto('/lecturer/classes');
    const card = page.locator('.lecturer-class-card').filter({hasText: BASE.classroom}).first();
    await expect(card, `Không thấy lớp "${BASE.classroom}" - seed chưa nạp`).toBeVisible();

    const href = await card.locator('a[href*="/members"]').first().getAttribute('href');
    return href.match(/\/lecturer\/classes\/(\d+)\/members/)[1];
}

async function findClassroomIdAsStudent(page) {
    await page.goto('/student/classrooms');
    const card = page.locator('.student-class-card').filter({hasText: BASE.classroom}).first();
    await expect(card, 'SV chưa phải thành viên lớp').toBeVisible();

    const href = await card.locator('a[href*="/roadmap"]').first().getAttribute('href');
    return href.match(/\/student\/classrooms\/(\d+)\/roadmap/)[1];
}

test('Login giảng viên', async ({page}) => {
    await login(page, lecturer);
    await expect(page).toHaveURL(/\/lecturer\/dashboard/);
});

test('Đăng ký sinh viên mới', async ({page}) => {
    const student = newStudent();
    await register(page, student);
    await login(page, student);
    await expect(page).toHaveURL(/\/student\/dashboard/);
});

test('Sinh viên xin vào lớp', async ({page}) => {
    const student = newStudent();
    await register(page, student);
    await login(page, student);

    await page.goto('/student/classrooms');
    await page.locator('#inviteCode').fill(BASE.inviteCode);
    await page.locator('form[action$="/join"] button[type="submit"]').click();

    await expect(page.locator('.alert-success')).toContainText(
        'Đã gửi yêu cầu tham gia lớp. Chờ giảng viên chấp nhận.'
    );

    await expect(page.locator('.student-pending-badge').first()).toBeVisible();
});

test('Sinh viên xin vào lớp, Giảng viên duyệt', async ({browser}) => {
    const student = newStudent();
    const studentPage = await browser.newPage();
    const lecturerPage = await browser.newPage();

    try {
        await register(studentPage, student);
        await login(studentPage, student);

        await studentPage.goto('/student/classrooms');
        await studentPage.locator('#inviteCode').fill(BASE.inviteCode);
        await studentPage.locator('form[action$="/join"] button[type="submit"]').click();

        await expect(studentPage.locator('.alert-success')).toContainText(
            'Đã gửi yêu cầu tham gia lớp. Chờ giảng viên chấp nhận.'
        );

        await login(lecturerPage, lecturer);
        await expect(lecturerPage).toHaveURL(/\/lecturer\/dashboard/);

        const classroomId = await findClassroomIdAsLecturer(lecturerPage);
        await lecturerPage.goto(`/lecturer/classes/${classroomId}/members`);

        const row = lecturerPage.locator('tr', {hasText: student.email});
        await expect(row, 'Yêu cầu của SV phải nằm ở danh sách chờ').toBeVisible();
        await row.locator('form[action*="/approve"] button[type="submit"]').click();

        await expect(lecturerPage.locator('.alert-success')).toContainText('Đã chấp nhận yêu cầu tham gia.');
        await expect(lecturerPage.locator('tr', {hasText: student.email}).locator('form[action*="/assign-ta"]'))
            .toBeVisible();

        const cid = await findClassroomIdAsStudent(studentPage);
        expect(cid, 'ID lớp nhìn từ SV và từ GV phải khớp').toBe(classroomId);

        await studentPage.goto(`/student/classrooms/${cid}/roadmap`);
        await expect(studentPage.locator('.hero-title')).toContainText(BASE.classroom);
    } finally {
        await studentPage.close();
        await lecturerPage.close();
    }
});
