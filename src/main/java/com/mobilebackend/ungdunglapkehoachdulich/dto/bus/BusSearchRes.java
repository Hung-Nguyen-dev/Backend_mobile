package com.mobilebackend.ungdunglapkehoachdulich.dto.bus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusSearchRes {
    private String query;
    private List<BusSearchItemRes> items;
}
