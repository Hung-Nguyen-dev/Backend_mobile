package com.mobilebackend.ungdunglapkehoachdulich.service;

import org.springframework.stereotype.Service;

@Service
public class LocalAiService {

    public String generateReply(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "Xin chao ban! Ban hay cho minh biet diem den va so ngay de minh goi y lich trinh phu hop nhe.";
        }

        String p = prompt.toLowerCase();
        if (p.contains("ha noi") || p.contains("hà nội")) {
            return "Chao ban! Neu ban di Ha Noi, minh goi y Van Mieu - Quoc Tu Giam, Hoan Kiem, Pho co va mot bua toi thuong thuc bun cha. Ban muon lich trinh 1 ngay hay 2 ngay de minh sap xep chi tiet?";
        }

        return "Chao ban! Minh da nhan thong tin va se goi y lich trinh dua tren diem den, ngan sach va so ngay ban di.";
    }
}
