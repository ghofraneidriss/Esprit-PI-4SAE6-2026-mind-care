package tn.esprit.medical_report_service.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import tn.esprit.medical_report_service.DTOs.MRIScanDTO;
import tn.esprit.medical_report_service.Enteties.MRIScan;

@Mapper(componentModel = "spring", uses = { AIResultMapper.class }, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MRIScanMapper {
    @Mapping(target = "medicalReportId", source = "medicalReport.reportid")
    @Mapping(target = "aiResult", source = "aiResult")
    MRIScanDTO toDTO(MRIScan mriScan);

    @Mapping(target = "medicalReport", ignore = true)
    @Mapping(target = "aiResult", ignore = true)
    MRIScan toEntity(MRIScanDTO mriScanDTO);
}
