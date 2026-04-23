package tn.esprit.medical_report_service.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import tn.esprit.medical_report_service.DTOs.MedicalReportDTO;
import tn.esprit.medical_report_service.Enteties.MedicalReport;

@Mapper(componentModel = "spring", uses = { FileMapper.class, MRIScanMapper.class }, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedicalReportMapper {
    @Mapping(target = "id", source = "reportid")
    @Mapping(target = "patientId", source = "patientid")
    @Mapping(target = "doctorId", source = "doctorid")
    @Mapping(target = "reportDate", source = "approvedAt")
    MedicalReportDTO toDTO(MedicalReport medicalReport);

    @Mapping(target = "reportid", source = "id")
    @Mapping(target = "patientid", source = "patientId")
    @Mapping(target = "doctorid", source = "doctorId")
    @Mapping(target = "approvedAt", source = "reportDate")
    MedicalReport toEntity(MedicalReportDTO medicalReportDTO);

    @Mapping(target = "reportid", source = "id")
    @Mapping(target = "patientid", source = "patientId")
    @Mapping(target = "doctorid", source = "doctorId")
    @Mapping(target = "approvedAt", source = "reportDate")
    void updateEntityFromDTO(MedicalReportDTO dto, @MappingTarget MedicalReport entity);
}
