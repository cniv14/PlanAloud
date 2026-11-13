package app.planaloud.assistant;

import android.content.Context;
import android.view.View;

/** Lets the embedded conversation reuse the host's live backdrop without a module dependency cycle. */
public interface ConversationAppearance {
    View createConversationBackdrop(Context context);
}
