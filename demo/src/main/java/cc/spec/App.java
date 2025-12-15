package cc.spec;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) {
        System.out.println("Hello World!");
        // Test OpenAIClient usage
        try {
            OpenAIClient openAIClient = new OpenAIClient();
            String prompt = "Say hello from OpenAI!";
            String response = openAIClient.chatCompletion(prompt);
            System.out.println("OpenAI API response: " + response);
        } catch (Exception e) {
            System.err.println("OpenAI API call failed: " + e.getMessage());
        }
    }
}
