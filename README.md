# 🏥 Electronic Health Management System (EHMS)

EHMS là một hệ thống quản lý sức khỏe điện tử được thiết kế nhằm hỗ trợ các cơ sở y tế như bệnh viện, phòng khám và trung tâm y tế trong việc quản lý bệnh nhân, lịch hẹn, đơn thuốc và thông tin nhân viên y tế một cách hiệu quả và bảo mật.

## 🚀 Tính năng chính

- 📋 **Quản lý hồ sơ bệnh nhân**  
  Lưu trữ và chỉnh sửa thông tin bệnh nhân như họ tên, ngày sinh, giới tính, tiền sử bệnh, v.v.

- 💊 **Đơn thuốc và chẩn đoán**  
  Ghi lại kết quả khám và đơn thuốc được kê cho từng lần khám.

- 👩‍⚕️ **Quản lý nhân sự y tế**  
  Tạo tài khoản  cho bác sĩ, dược sĩ , ... 

- 🔐 **Bảo mật & phân quyền**  
  Đăng nhập theo vai trò (admin, bác sĩ,) và bảo mật thông tin bệnh nhân.

- 🖥️ **Giao diện thân thiện**  
  Giao diện người dùng hiện đại, dễ sử dụng, hỗ trợ đa dạng khung giờ & ngày tháng.

---

## 🛠️ Công nghệ sử dụng

| Thành phần        | Công nghệ                          |
|------------------|-----------------------------------|
| **Ngôn ngữ**     | Java                              |
| **Build tool**   | Maven                             |
| **Cơ sở dữ liệu**| PostgreSQL                         |
| **Giao diện**    | FlatLaf, jCalendar, LGoodDatePicker |
| **Logging**      | SLF4J                              |

---

## 🧰 Cài đặt & chạy ứng dụng


### 1. Clone dự án
```bash
git clone https://github.com/baokhanh546123/Electronic-Health-Management-System-ehms.git
cd Electronic-Health-Management-System-ehms
```

### 2.Chạy dự án bằng file JAR trong mục target
```
cd .\Electronic-Health-Management-System-ehms\target
java -jar ehms-1.0-SNAPSHOT-fat.jar
```
