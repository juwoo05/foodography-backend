package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecipeDTO(

        Integer recipeId,

        List<String> ingredients,

        String title,

        String content,

        String difficulty,

        Integer cooking_time,

        Integer calories,

        String youtube_url,

        String youtube_url_thumbnail,

        String scanId
) {
}
