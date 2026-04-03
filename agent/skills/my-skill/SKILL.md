---
name: business_logic_implementer
description: Skill này chịu trách nhiệm chuyển đổi các yêu cầu nghiệp vụ (Business Requirements) thành mã nguồn thực tế ở tầng Service Layer. Nó không tập trung vào kết nối DB hay Routing API, mà tập trung vào luồng xử lý dữ liệu, tính toán, và thực hiện các quy tắc nghiệp vụ (Business Rules).
---

Capabilities
    Xử lý Logic: Viết các hàm xử lý tính toán, kiểm tra điều kiện (validation) và điều phối dữ liệu.
    Framework Support:
        Spring Boot: Viết các @Service class, xử lý nghiệp vụ giữa Repository và Controller.
    Error Handling: Định nghĩa các ngoại lệ (Custom Exceptions) phù hợp với nghiệp vụ.

Guidelines & Constraints
    Clean Code: Tuân thủ nguyên tắc SOLID. Mỗi hàm chỉ nên làm một việc duy nhất.
    Framework Identity:

    Nếu là Spring Boot, tuyệt đối không gọi trực tiếp các lệnh SQL trong tầng Service (phải qua Repository).
    Security: Luôn kiểm tra quyền hạn và tính hợp lệ của dữ liệu đầu vào trước khi xử lý.
    Performance: Ưu tiên các thuật toán xử lý mảng/list tối ưu, tránh vòng lặp lồng nhau gây chậm hệ thống.