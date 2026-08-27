Write-Host "======================================="
Write-Host "BẮT ĐẦU CHUỖI KIỂM THỬ HIỆU NĂNG"
Write-Host "======================================="

Write-Host "`n[1/3] Đang chạy KIỂM THỬ TẢI (Load Test) - 5 Phút..."
.\k6-v0.53.0-windows-amd64\k6.exe run --insecure-skip-tls-verify --out web-dashboard=export=report-load.html -e TEST_TYPE=load main-test.js
Write-Host ">> Đã xuất báo cáo: report-load.html"

Write-Host "`nĐang làm mát Server 15 giây..."
Start-Sleep -Seconds 15

Write-Host "`n[2/3] Đang chạy KIỂM THỬ ĐỘT BIẾN (Spike Test) - 2 Phút..."
.\k6-v0.53.0-windows-amd64\k6.exe run --insecure-skip-tls-verify --out web-dashboard=export=report-spike.html -e TEST_TYPE=spike main-test.js
Write-Host ">> Đã xuất báo cáo: report-spike.html"

Write-Host "`nĐang làm mát Server 15 giây..."
Start-Sleep -Seconds 15

Write-Host "`n[3/3] Đang chạy KIỂM THỬ CĂNG THẲNG (Stress Test) - 10 Phút..."
.\k6-v0.53.0-windows-amd64\k6.exe run --insecure-skip-tls-verify --out web-dashboard=export=report-stress.html -e TEST_TYPE=stress main-test.js
Write-Host ">> Đã xuất báo cáo: report-stress.html"

Write-Host "`n======================================="
Write-Host "HOÀN THẤT! Đã xuất đủ 3 file báo cáo HTML."
Write-Host "======================================="
