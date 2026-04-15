package com.mobilebackend.ungdunglapkehoachdulich.dto.bus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusSearchItemRes {
    private String title;
    private String snippet;
    private String link;
}
