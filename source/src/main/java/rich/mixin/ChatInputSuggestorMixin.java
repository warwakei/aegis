package rich.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.events.api.EventManager;
import rich.events.impl.TabCompleteEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {

    @Shadow
    @Final
    TextFieldWidget textField;

    @Shadow
    @Final
    private List<OrderedText> messages;

    @Shadow
    private ParseResults<?> parse;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    private ChatInputSuggestor.SuggestionWindow window;

    @Shadow
    boolean completingSuggestions;

    @Shadow
    public abstract void show(boolean narrateFirstSuggestion);

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    private void onRefresh(CallbackInfo ci) {
        String text = this.textField.getText();
        int cursor = this.textField.getCursor();
        String prefix = text.substring(0, Math.min(text.length(), cursor));

        TabCompleteEvent event = new TabCompleteEvent(prefix);
        EventManager.callEvent(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        if (event.completions != null) {
            ci.cancel();

            this.parse = null;

            if (this.completingSuggestions) {
                return;
            }

            this.textField.setSuggestion(null);
            this.window = null;
            this.messages.clear();

            if (event.completions.length == 0) {
                this.pendingSuggestions = Suggestions.empty();
            } else {
                int lastSpace = prefix.lastIndexOf(' ');
                StringRange range = StringRange.between(lastSpace + 1, prefix.length());

                List<Suggestion> suggestionList = Stream.of(event.completions)
                        .map(s -> new Suggestion(range, s))
                        .collect(Collectors.toList());

                Suggestions suggestions = new Suggestions(range, suggestionList);

                this.pendingSuggestions = new CompletableFuture<>();
                this.pendingSuggestions.complete(suggestions);
            }

            this.show(true);
        }
    }
}