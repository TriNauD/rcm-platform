package com.bank.rcm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bank.rcm.repository.ControlExpectationRepository;
import com.bank.rcm.repository.ObCeamMappingRepository;
import com.bank.rcm.repository.ObCesMappingRepository;

@Service
public class ComplianceStepBService {
    @Autowired
    private CubeInternalSerice cubeInternalSerice;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private ControlExpectationRepository cesRepo;

    @Autowired
    private ObCeamMappingRepository obCeamMappingRepo;

    @Autowired
    private ObCesMappingRepository obCesMappingRepo;
}
