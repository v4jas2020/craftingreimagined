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

    /**
     * * Called whenever the search text changes.
     *
     */
    private void onSearchChanged(String query) {

        searchListener.accept(query);
    }

    /**
     * * Sets the callback used when the player
     * * types something into the search field.
     *
     */
    public void setSearchListener(
            Consumer<String> listener) {

        this.searchListener = listener != null
                ? listener : query -> {
        };
    }

    /**
     * * Returns the current search query.
     *
     */
    public String getQuery() {

        return getValue();
    }

    /**
     * * Sets the search query programmatically.
     *
     */
    public void setQuery(String query) {

        setValue(query == null ? "" : query);
    }

    /**
     * * Clears the search field.
     *
     */
    public void clearSearch() {

        setValue("");
    }

    /**
     * * Checks whether the search field is empty.
     *
     */
    public boolean isSearchEmpty() {

        return getValue().isBlank();
    }

    /**
     * * Returns matching recipes using RecipeIndex.
     *
     */
    public List<RecipeEntry> getMatchingRecipes() {

        return RecipeIndex.search(getValue());
    }

    /**
     * * Returns the number of matching recipes.
     *
     */
    public int getMatchCount() {

        return getMatchingRecipes().size();
    }
}

