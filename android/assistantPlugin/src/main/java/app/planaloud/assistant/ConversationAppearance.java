package app.planaloud.assistant;

import android.content.Context;
import android.view.View;

/** Lets the embedded conversation reuse the host's live backdrop without a module dependency cycle. */
public interface ConversationAppearance {
    View createConversationBackdrop(Context context);
    default boolean applyConversationTheme(android.app.Activity activity){return false;}
    default void recordConversationError(String operation,Throwable failure){}
    default void styleConversationSurface(View view,boolean own){}

}
