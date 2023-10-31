import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.stream.Stream;

import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.*;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class BookingComSelenideTest {

    @BeforeAll
    public static void setUp() {
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide().screenshots(true).savePageSource(true));
    }

    @BeforeEach
    public void openBrowser() {
        open("https://www.booking.com");
        getWebDriver().manage().window().maximize();
        if ($(byXpath("//button[@aria-label='Dismiss sign-in info.']")).exists()) {
            $(byXpath("//button[@aria-label='Dismiss sign-in info.']")).click();
            $(byXpath("//button[@aria-label='Dismiss sign-in info.']")).shouldNot(exist);
        }
    }

    @Test
    public void mainPage() {
        $(byText("Find your next stay")).shouldBe(visible);
        $("input#\\:re\\:").shouldHave(attribute("placeholder", "Where are you going?"));
        $("[data-testid='searchbox-dates-container']").shouldHave(text("Check-in date — Check-out date"));
        $("[data-testid='occupancy-config']").shouldHave(text("2 adults · 0 children · 1 room"));
        $(byTagAndText("button", "Search")).shouldBe(visible);
    }

    @DisplayName("Checking location autocomplete results:")
    @ParameterizedTest(name = "Typed in ''{0}'' displayed: {1} {2}")
    @CsvSource({
            "Tel, Tel Aviv, 'Center District Israel, Israel'",
            "Eil, Eilat, 'South District Israel, Israel'",
            "Mosc, Moscow, Russia",
            "Tbili, Tbilisi, 'Tbilisi Region, Georgia'"
    })
    public void locationChoice(String locationInput, String area, String region){
        $("input#\\:re\\:").setValue(locationInput);
        $$("[data-testid='autocomplete-results'] ul:first-child li").shouldHave(size(5));
        $$("[data-testid='autocomplete-results'] ul:first-child li").shouldHave(itemWithText(area + "\n" + region));
        $("input#\\:re\\:").pressEnter();
        $("input#\\:re\\:").shouldHave(text(area));
    }

    @DisplayName("Checking occupancy options")
    @ParameterizedTest(name = "{0} adults , {1} children, {2} rooms")
    @MethodSource("occupancyInfoProvider")
    public void occupancyChoice(int adults, int children, int rooms){
        $("[data-testid='occupancy-config']").shouldHave(text("2 adults · 0 children · 1 room"));
        $("[data-testid='occupancy-config']").click();
        $("[data-testid='occupancy-popup']").shouldBe(visible);
        $(byXpath("//input[@id='group_adults']/following-sibling::div/button[1]")).click();
        for (int i = 0; i < adults; i++) {
            $(byXpath("//input[@id='group_adults']/following-sibling::div/button[2]")).click();
        }
        for (int i = 0; i < children; i++) {
            $(byXpath("//input[@id='group_children']/following-sibling::div/button[2]")).click();
        }
        Random rand = new Random();
        $$("[data-testid='kids-ages-select'] select").asFixedIterable().forEach(e -> e.selectOption(rand.nextInt(16) + 1));
        for (int i = 0; i < rooms; i++) {
            $(byXpath("//input[@id='no_rooms']/following-sibling::div/button[2]")).click();
        }
        $("[data-testid='occupancy-config']")
                .shouldHave(text(String.format("%d adults · %d children · %d room", adults + 1, children, rooms + 1)));
    }

    static Stream<Arguments> occupancyInfoProvider(){
        return Stream.generate(() -> {
            Random rand = new Random();
            return arguments(rand.nextInt(30),rand.nextInt(11), rand.nextInt(30));})
                .limit(3);

    }

    @Test
    public void pickingDates(){
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(5);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE, MMM d");
        $("[data-testid='searchbox-dates-container']").click();
        $(String.format("span[data-date='%s']", startDate)).click();
        $(String.format("span[data-date='%s']", endDate)).click();
        $("[data-testid='searchbox-dates-container']")
                .shouldHave(text(f.format(startDate) + "\n" + " — " + "\n" + f.format(endDate)));

    }


}
