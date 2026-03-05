package tn.esprit.medical_report_service.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import tn.esprit.medical_report_service.DTOs.MedicalReportDTO;
import tn.esprit.medical_report_service.Enteties.MedicalReport;

@Mapper(componentModel = "spring", uses = { FileMapper.class,
        MRIScanMapper.class }, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedicalReportMapper {
    MedicalReportDTO toDTO(MedicalReport medicalReport);

    MedicalReport toEntity(MedicalReportDTO medicalReportDTO);

    void updateEntityFromDTO(MedicalReportDTO dto, @MappingTarget MedicalReport entity);
}
