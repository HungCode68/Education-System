import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// Base URL cho server mục tiêu
// Hãy đổi thành IP/Domain thật của server 1 Core/4GB RAM trước khi test.
const BASE_URL = __ENV.BASE_URL || 'https://103.57.220.127/api/v1';

// Lựa chọn kỹ thuật test qua biến môi trường (Mặc định là load)
const TEST_TYPE = __ENV.TEST_TYPE || 'load';

// Cấu hình các loại tải (Phân bổ 20% Quản lý, 80% Học viên)
const testConfig = {
    load: {
        admin_scenario: {
            executor: 'ramping-vus',
            exec: 'adminFlow',
            stages: [
                { duration: '1m', target: 4 }, 
                { duration: '3m', target: 4 },
                { duration: '1m', target: 0 },
            ],
        },
        student_scenario: {
            executor: 'ramping-vus',
            exec: 'studentFlow',
            stages: [
                { duration: '1m', target: 16 }, 
                { duration: '3m', target: 16 },
                { duration: '1m', target: 0 },
            ],
        }
    },
    short_load: {
        admin_scenario: {
            executor: 'ramping-vus',
            exec: 'adminFlow',
            stages: [
                { duration: '10s', target: 4 }, 
                { duration: '40s', target: 4 },
                { duration: '10s', target: 0 },
            ],
        },
        student_scenario: {
            executor: 'ramping-vus',
            exec: 'studentFlow',
            stages: [
                { duration: '10s', target: 16 }, 
                { duration: '40s', target: 16 },
                { duration: '10s', target: 0 },
            ],
        }
    },
    stress: {
        admin_scenario: {
            executor: 'ramping-vus',
            exec: 'adminFlow',
            stages: [
                { duration: '2m', target: 4 },
                { duration: '2m', target: 8 },
                { duration: '2m', target: 12 },
                { duration: '2m', target: 16 },
                { duration: '2m', target: 20 },
            ]
        },
        student_scenario: {
            executor: 'ramping-vus',
            exec: 'studentFlow',
            stages: [
                { duration: '2m', target: 16 },
                { duration: '2m', target: 32 },
                { duration: '2m', target: 48 },
                { duration: '2m', target: 64 },
                { duration: '2m', target: 80 },
            ]
        }
    },
    spike: {
        admin_scenario: {
            executor: 'ramping-vus',
            exec: 'adminFlow',
            stages: [
                { duration: '1m', target: 1 },
                { duration: '10s', target: 20 },
                { duration: '30s', target: 20 },
                { duration: '10s', target: 1 },
            ]
        },
        student_scenario: {
            executor: 'ramping-vus',
            exec: 'studentFlow',
            stages: [
                { duration: '1m', target: 4 },
                { duration: '10s', target: 80 },
                { duration: '30s', target: 80 },
                { duration: '10s', target: 4 },
            ]
        }
    },
    debug: {
        student_scenario: {
            executor: 'shared-iterations',
            exec: 'studentFlow',
            vus: 1,
            iterations: 1,
        }
    }
};

export const options = {
    scenarios: testConfig[TEST_TYPE],
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 95% request phải phản hồi dưới 2s
        http_req_failed: ['rate<0.05'], // Tỉ lệ lỗi chung phải dưới 5%
    },
};

// --- HÀM HỖ TRỢ ---
function login(email, password) {
    const payload = JSON.stringify({ email: email, password: password });
    const res = http.post(`${BASE_URL}/auth/login`, payload, {
        headers: { 'Content-Type': 'application/json' }
    });
    
    check(res, { 'Login successful': (r) => r.status === 200 });
    
    // Nếu API login trả về accessToken trong body
    return res.status === 200 ? res.json('accessToken') : null;
}

// --- LUỒNG 1: QUẢN LÝ (ADMIN) ---
export function adminFlow() {
    // 1. Đăng nhập (Vui lòng chuẩn bị sẵn user admin này trong DB)
    const token = login('dt260001@school.edu.vn', 'DT260001'); 
    
    // 2. Think time (1-3s)
    sleep(Math.random() * 2 + 1);

    // 3. Xem báo cáo thống kê
    if (token) {
        group('Admin views Center Statistics', () => {
            const res = http.get(`${BASE_URL}/reporting/statistics/latest`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            check(res, { 'Report loaded successfully': (r) => r.status === 200 });
        });
    }
}

// --- LUỒNG 2: HỌC VIÊN (STUDENT) ---
export function studentFlow() {
    // k6 cung cấp biến __VU (Virtual User ID), chạy từ 1 đến N
    // Chúng ta format số này thành dạng 4 chữ số và bắt đầu từ user 0003 (Nghĩa là __VU + 2)
    const vuNumber = __VU + 2;
    const paddedVu = vuNumber.toString().padStart(4, '0');
    const email = `hv26${paddedVu}@student.edu.vn`;
    const password = `HV26${paddedVu}`;

    // 1. Đăng nhập
    const token = login(email, password); 
    
    // 2. Think time (2s)
    sleep(2);

    if (token) {
        group('Student does Assignment', () => {
            const params = { headers: { 'Authorization': `Bearer ${token}` } };
            
            // 3. Lấy danh sách lớp học của học viên
            const classRes = http.get(`${BASE_URL}/classes/my-classes`, params);
            check(classRes, { 'Fetched classes successfully': (r) => r.status === 200 });
            
            if (classRes.status === 200) {
                const classes = classRes.json();
                if (classes && classes.length > 0) {
                    // Chọn lớp đầu tiên
                    const classId = classes[0].id;
                    
                    // 4. Lấy danh sách bài tập của lớp đó
                    const assignRes = http.get(`${BASE_URL}/assignments/class/${classId}`, params);
                    check(assignRes, { 'Fetched assignments successfully': (r) => r.status === 200 });
                    
                    if (assignRes.status === 200) {
                        const assignments = assignRes.json();
                        if (assignments && assignments.length > 0) {
                            // Cố định dùng ID 4 theo yêu cầu vì ID 5 đã bị khóa
                            const assignmentToDo = assignments.find(a => a.id === 4) || assignments.find(a => a.status === 'PUBLISHED') || assignments[0];
                            const assignmentId = assignmentToDo.id; 
                            
                            // 5. Bắt đầu làm bài
                            const startRes = http.post(`${BASE_URL}/submissions/start/${assignmentId}`, null, params);
                            
                            if (startRes.status !== 201) {
                                console.log(`Failed to start assignment ${assignmentId} - Status: ${startRes.status} - Body: ${startRes.body}`);
                            }

                            // Lưu ý: Nếu bài tập đã làm rồi có thể trả về 400. Ở đây check 201 (Tạo mới).
                            check(startRes, { 'Started assignment successfully': (r) => r.status === 201 });
                            
                            if (startRes.status === 201 || startRes.status === 400) {
                                const submissionId = startRes.json('data.id');

                                // 6. Think time (Giả lập thời gian làm bài: 5 - 10s)
                                sleep(Math.random() * 5 + 5);

                                // 7. Nộp bài
                                const submitRes = http.post(`${BASE_URL}/submissions/submit/${submissionId}`, null, params);
                                check(submitRes, { 'Submitted assignment successfully': (r) => r.status === 200 });
                            }
                        }
                    }
                }
            }
        });
    }
}

// --- HÀM XUẤT BÁO CÁO (CHẠY CUỐI CÙNG KHI TEST XONG) ---
export function handleSummary(data) {
    return {
        // In tóm tắt ra console
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
