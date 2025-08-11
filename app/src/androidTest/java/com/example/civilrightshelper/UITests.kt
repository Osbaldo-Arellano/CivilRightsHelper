package com.example.civilrightshelper

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chatScreen_displaysTitle() {
        composeTestRule.onNodeWithText("Civil Rights Helper").assertIsDisplayed()
    }

    @Test
    fun clickingInfoIcon_navigatesToInfoScreen() {
        // Click the Info icon
        composeTestRule.onNode(hasContentDescription("Info")).performClick()

        // Info screen title is shown
        composeTestRule.onNodeWithText("App Info").assertIsDisplayed()
    }

    @Test
    fun infoScreen_selectsSpanishLanguage() {
        // Go to info screen
        composeTestRule.onNodeWithContentDescription("Info").performClick()

        // Click the Spanish option
        composeTestRule.onNodeWithText("Spanish").performClick()

        //Spanish button is selected
        composeTestRule.onAllNodes(isSelectable() and isSelected())
            .filter(hasAnySibling(hasText("Spanish")))
            .assertCountEquals(1)
    }

    @Test
    fun sendingMessage_addsUserBubble() {
        val messageText = "What are my rights?"

        // Type a message
        composeTestRule.onNodeWithText("Type your question").performTextInput(messageText)

        // Click Send
        composeTestRule.onNodeWithText("Send").performClick()

        // message bubble exists
        composeTestRule.onNodeWithText(messageText).assertIsDisplayed()
    }

    @Test
    fun backFromInfo_returnsToChat() {
        // Navigate to Info
        composeTestRule.onNode(hasContentDescription("Info")).performClick()

        // Click Back arrow
        composeTestRule.onNode(hasContentDescription("Back")).performClick()

        // we're back at chat
        composeTestRule.onNodeWithText("Civil Rights Helper").assertIsDisplayed()
    }
}
