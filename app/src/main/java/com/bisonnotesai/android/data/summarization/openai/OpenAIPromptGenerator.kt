package com.bisonnotesai.android.data.summarization.openai

import com.bisonnotesai.android.domain.model.ContentType

/**
 * OpenAI prompt generator for different summarization tasks
 * Ported from iOS OpenAIPromptGenerator.swift
 */
object OpenAIPromptGenerator {

    enum class PromptType {
        SUMMARY,
        TASKS,
        REMINDERS,
        TITLES,
        COMPLETE,
        CONTENT_CLASSIFICATION
    }

    /**
     * Create system prompt for the given type
     */
    fun createSystemPrompt(type: PromptType, contentType: ContentType = ContentType.GENERAL): String {
        val basePrompt = """
            You are an AI assistant specialized in analyzing and summarizing audio transcripts and conversations. Your role is to provide clear, actionable insights from the content provided.

            **Key Guidelines:**
            - Focus on extracting meaningful, actionable information
            - Maintain accuracy and relevance to the source material
            - Use clear, professional language
            - Structure responses logically and coherently
            - Prioritize the most important information first
        """.trimIndent()

        val contentTypePrompt = createContentTypeSpecificPrompt(contentType)

        return when (type) {
            PromptType.SUMMARY -> "$basePrompt\n\n$contentTypePrompt\n\n${createSummaryPrompt()}"
            PromptType.TASKS -> "$basePrompt\n\n${createTasksPrompt()}"
            PromptType.REMINDERS -> "$basePrompt\n\n${createRemindersPrompt()}"
            PromptType.TITLES -> "$basePrompt\n\n${createTitlesPrompt()}"
            PromptType.COMPLETE -> "$basePrompt\n\n$contentTypePrompt\n\n${createCompletePrompt()}"
            PromptType.CONTENT_CLASSIFICATION -> "$basePrompt\n\n${createContentClassificationPrompt()}"
        }
    }

    /**
     * Create content type specific prompt
     */
    private fun createContentTypeSpecificPrompt(contentType: ContentType): String {
        return when (contentType) {
            ContentType.MEETING -> """
                **Meeting Analysis Focus:**
                - Identify key decisions and action items
                - Note important deadlines and commitments
                - Highlight participant responsibilities
                - Capture meeting outcomes and next steps
                - Focus on business-relevant information
            """.trimIndent()

            ContentType.LECTURE -> """
                **Lecture Analysis Focus:**
                - Identify main concepts and learning objectives
                - Note key definitions and terminology
                - Highlight important examples and case studies
                - Capture essential knowledge and insights
                - Focus on educational content
            """.trimIndent()

            ContentType.INTERVIEW -> """
                **Interview Analysis Focus:**
                - Identify key questions and responses
                - Note candidate qualifications and experience
                - Highlight important insights and reactions
                - Capture decision factors and impressions
                - Focus on assessment-relevant information
            """.trimIndent()

            ContentType.CONVERSATION -> """
                **Conversation Analysis Focus:**
                - Identify main topics discussed
                - Note important agreements or decisions
                - Highlight key points and takeaways
                - Capture context and relevant details
                - Focus on meaningful exchanges
            """.trimIndent()

            ContentType.PRESENTATION -> """
                **Presentation Analysis Focus:**
                - Identify main themes and key messages
                - Note important data and statistics
                - Highlight conclusions and recommendations
                - Capture visual elements descriptions
                - Focus on core presentation content
            """.trimIndent()

            ContentType.GENERAL -> """
                **General Analysis Focus:**
                - Identify main topics and themes
                - Note important information and insights
                - Highlight key points and takeaways
                - Capture relevant details and context
                - Focus on clarity and comprehensiveness
            """.trimIndent()
        }
    }

    /**
     * Create summary prompt
     */
    private fun createSummaryPrompt(): String {
        return """
            **Summary Generation Guidelines:**
            - Create a comprehensive summary using Markdown formatting (aim for 15-20% of the original transcript length)
            - Use **bold** for key points and important information
            - Use *italic* for emphasis and highlights
            - Use ## headers for main sections
            - Use ### subheaders for subsections
            - Use • bullet points for lists and key takeaways
            - Use > blockquotes for important statements or quotes
            - Keep the summary well-structured and informative
            - Focus on the most important content and insights
            - Balance comprehensiveness with conciseness
            - Ensure the summary captures the essence of the conversation
        """.trimIndent()
    }

    /**
     * Create tasks prompt
     */
    private fun createTasksPrompt(): String {
        return """
            **Task Extraction Guidelines:**
            - Identify actionable tasks and to-dos mentioned in the conversation
            - Focus on items that require follow-up or action
            - Include specific deadlines or time references when mentioned
            - Assign priority levels (high, medium, low, urgent) based on urgency and importance
            - Only include tasks with clear action items
            - Avoid general statements that don't require specific action
            - Extract who is responsible for each task if mentioned
        """.trimIndent()
    }

    /**
     * Create reminders prompt
     */
    private fun createRemindersPrompt(): String {
        return """
            **Reminder Extraction Guidelines:**
            - Identify time-sensitive items and deadlines
            - Focus on appointments, meetings, and scheduled events
            - Include specific dates, times, or time references
            - Categorize importance appropriately (high, medium, low)
            - Only include items that require timely attention
            - Avoid general information that doesn't have a time component
            - Focus on items that would benefit from a reminder
        """.trimIndent()
    }

    /**
     * Create titles prompt
     */
    private fun createTitlesPrompt(): String {
        return """
            **Title Generation Guidelines:**
            - Generate 3 concise, descriptive titles (20-50 characters, 3-8 words each)
            - Capture the main topic, purpose, or key subject
            - Be specific and meaningful - avoid generic terms
            - Focus on the most important subject, person, or action mentioned
            - Use proper capitalization (Title Case)
            - Never end with punctuation marks
            - Make titles work well as file names or conversation titles
            - Be logical and sensible - make it clear what the content is about
            - Provide a confidence score (0.0-1.0) for each title

            **Examples of good titles:**
            - "Project Budget Review"
            - "Client Presentation Prep"
            - "Team Strategy Meeting"
            - "Quarterly Sales Report"
            - "Product Launch Planning"
        """.trimIndent()
    }

    /**
     * Create complete prompt
     */
    private fun createCompletePrompt(): String {
        return """
            **Complete Analysis Guidelines:**
            - Provide a comprehensive analysis in a single response
            - Include summary, tasks, reminders, and titles
            - Use the standardized title generation logic
            - Ensure all components are properly formatted as JSON
            - Focus on actionable insights and meaningful information
            - Maintain consistency across all extracted elements

            **JSON Response Format:**
            {
              "summary": "Markdown formatted summary",
              "tasks": [
                {
                  "text": "Task description",
                  "priority": "high|medium|low|urgent",
                  "assignee": "Person name if mentioned",
                  "due_date": "YYYY-MM-DD if mentioned"
                }
              ],
              "reminders": [
                {
                  "text": "Reminder description",
                  "date": "YYYY-MM-DD HH:mm if mentioned",
                  "importance": "high|medium|low"
                }
              ],
              "titles": [
                {
                  "text": "Title suggestion",
                  "confidence": 0.9
                }
              ],
              "content_type": "meeting|lecture|interview|conversation|presentation|general"
            }
        """.trimIndent()
    }

    /**
     * Create content classification prompt
     */
    private fun createContentClassificationPrompt(): String {
        return """
            **Content Classification Guidelines:**
            - Analyze the content and classify it into one of these types:
              - meeting: Business meetings, discussions, team standups
              - lecture: Educational content, classes, training sessions
              - interview: Job interviews, candidate assessments
              - conversation: General conversations, informal discussions
              - presentation: Formal presentations, pitches, demos
              - general: Content that doesn't fit other categories
            - Base your decision on the content structure, language, and context
            - Respond with only the classification type
        """.trimIndent()
    }

    /**
     * Create user prompt for the given type
     */
    fun createUserPrompt(type: PromptType, text: String): String {
        return when (type) {
            PromptType.SUMMARY -> """
                Please provide a comprehensive summary of the following content using proper Markdown formatting:

                $text
            """.trimIndent()

            PromptType.TASKS -> """
                Please extract actionable tasks from the following content. Return as JSON array:

                $text
            """.trimIndent()

            PromptType.REMINDERS -> """
                Please extract time-sensitive reminders from the following content. Return as JSON array:

                $text
            """.trimIndent()

            PromptType.TITLES -> """
                Please generate 3 concise, descriptive titles for the following content. Return as JSON array with confidence scores:

                $text
            """.trimIndent()

            PromptType.COMPLETE -> """
                Please analyze the following content and provide a complete analysis with summary, tasks, reminders, titles, and content classification in JSON format:

                $text
            """.trimIndent()

            PromptType.CONTENT_CLASSIFICATION -> """
                Please classify the following content type:

                $text
            """.trimIndent()
        }
    }
}
