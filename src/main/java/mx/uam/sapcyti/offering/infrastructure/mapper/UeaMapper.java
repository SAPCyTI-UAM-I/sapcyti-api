package mx.uam.sapcyti.offering.infrastructure.mapper;

import mx.uam.sapcyti.offering.application.command.RegisterUeaCommand;
import mx.uam.sapcyti.offering.application.command.UpdateUeaCommand;
import mx.uam.sapcyti.offering.application.service.BulkUploadUeasUseCase;
import mx.uam.sapcyti.offering.application.service.RegisterUeaUseCase;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.RegisterUeaRequest;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.UpdateUeaRequest;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.UeaBulkUploadErrorResponse;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.UeaBulkUploadResultResponse;
import mx.uam.sapcyti.offering.infrastructure.adapter.in.dto.UeaCatalogItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UeaMapper {

    RegisterUeaCommand toCommand(RegisterUeaRequest request);

    UpdateUeaCommand toCommand(UpdateUeaRequest request);

    @Mapping(target = "horasTeoria", expression = "java(uea.getHorasTeoria().doubleValue())")
    @Mapping(target = "horasPractica", expression = "java(uea.getHorasPractica().doubleValue())")
    UeaCatalogItemResponse toResponse(UEA uea);

    @Mapping(target = "horasTeoria", expression = "java(result.getHorasTeoria().doubleValue())")
    @Mapping(target = "horasPractica", expression = "java(result.getHorasPractica().doubleValue())")
    UeaCatalogItemResponse toResponse(RegisterUeaUseCase.RegisterUeaResult result);

    UeaBulkUploadResultResponse toResponse(BulkUploadUeasUseCase.BulkUploadResult result);

    UeaBulkUploadErrorResponse toResponse(BulkUploadUeasUseCase.BulkUploadError error);
}
