package tn.esprit.medical_report_service.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import tn.esprit.medical_report_service.DTOs.AIResultDTO;
import tn.esprit.medical_report_service.Enteties.AIResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AIResultMapper {
    @Mapping(target = "mriScanId", source = "mriScan.id")
    AIResultDTO toDTO(AIResult aiResult);

    @Mapping(target = "mriScan", ignore = true)
    AIResult toEntity(AIResultDTO aiResultDTO);
}
