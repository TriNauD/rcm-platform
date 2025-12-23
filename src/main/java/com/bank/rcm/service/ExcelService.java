package com.bank.rcm.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bank.rcm.entity.RegulatoryInventory;

@Service
public class ExcelService {
    public List<RegulatoryInventory> mockParseExcel() {
        List<RegulatoryInventory> list = new ArrayList<>();

        RegulatoryInventory row1 = new RegulatoryInventory();
        row1.setPublicationId("PUBN-row1");
        row1.setPublicationName("row1");
        list.add(row1);

        RegulatoryInventory row2 = new RegulatoryInventory();
        row2.setPublicationId("PUBN-row2");
        row2.setPublicationName("row2");
        list.add(row2);
        
        return list;
    }

}
