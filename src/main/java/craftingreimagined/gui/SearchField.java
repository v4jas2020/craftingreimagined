package craftingreimagined.gui;

import craftingreimagined.recipe.RecipeEntry;
import craftingreimagined.recipe.RecipeIndex;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class SearchField extends EditBox {

    private static final int MAX_SEARCH_LENGTH = 100;

    private Consumer<String> searchListener;

    public SearchField(
            Font font,
            int x,
            int y,
            int width,
            int height
    ) {
        super(
                font,
                x,
                y,
                width,
                height,
                Component.literal("Recipe Search")
        );

        this.searchListener = query -> {
        };

        setMaxLength(MAX_SEARCH_LENGTH);

        setHint(
                Component.literal("Search recipes...")
        );

        setResponder(this::onSearchChanged);
    }


    private void onSearchChanged(String query) {

        searchListener.accept(query);
    }


    public void setSearchListener(
            Consumer<String> listener) {

        this.searchListener = listener != null
                ? listener : query -> {
        };
    }


    public String getQuery() {

        return getValue();
    }


    public void setQuery(String query) {

        setValue(query == null ? "" : query);
    }


    public void clearSearch() {

        setValue("");
    }


    public boolean isSearchEmpty() {

        return getValue().isBlank();
    }


    public List<RecipeEntry> getMatchingRecipes() {

        return RecipeIndex.search(getValue());
    }


    public int getMatchCount() {

        return getMatchingRecipes().size();
    }
}

