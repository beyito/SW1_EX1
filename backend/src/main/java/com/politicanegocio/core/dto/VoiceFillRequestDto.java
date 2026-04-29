package com.politicanegocio.core.dto;

import java.util.List;

public record VoiceFillRequestDto(
        String voiceTranscript,
        List<String> formFields
) {
}

