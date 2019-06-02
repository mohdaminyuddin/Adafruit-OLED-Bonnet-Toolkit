# Adafruit OLED Bonnet Toolkit
Java toolkit for the [Adafruit 128x64 OLED bonnet with buttons and D-pad](https://www.adafruit.com/product/3531).

This toolkit has support for drawing text or pixels on the OLED screen and receiving events from the D-pad and buttons. It also includes a UI layout library,
and some task scheduling classes to make it easy to build an asynchronous application that does not block the screen update thread.

To initialize the OLED screen and button hardware and start the UI, use the following pattern:

```java
public static void main(String[] args) throws Exception {
    // Initialize the Button class, and register the GPIO event listeners
    Bonnet.init();

    // Initialize UI with the RootScreen class
    Screen.init(new RootScreen());

    // Keep program running until termination
    for (;;) {
        try {
            Thread.sleep(1000_000);
        } catch (InterruptedException e) {
            break;
        }
    }
}
```

where `RootScreen` extends `Screen`, which is a display controller for a single tree of UI elements (similar to an `Activity` in Android):

```java
import aobtk.font.Font;
import aobtk.hw.HWButton;
import aobtk.ui.element.Menu;
import aobtk.ui.element.TextElement;
import aobtk.ui.element.VLayout;
import aobtk.ui.element.VLayout.VAlign;
import aobtk.ui.screen.Screen;

public class RootScreen extends Screen {

    private Menu menu;

    public TestScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void open() {
    	// Set up UI when screen opens
        VLayout layout = new VLayout();

        layout.add(new TextElement(Font.FONT_5X8, "Hello World"), VAlign.TOP);

        menu = new Menu(Font.FONT_5X8, /* spacing = */ 0, /* hLayout = */ true,
                "Hello", "Goodbye");
        layout.add(menu, VAlign.BOTTOM);

        setUI(layout);
    }

    @Override
    public void close() {
        // Teardown stuff here when screen closes
    }

    @Override
    public boolean acceptsButtonA() {
        // Button A will move up to parent screen by default
        return false;
    }

    @Override
    public void buttonDown(HWButton button) {
        switch (button) {
        case L:
            menu.decSelectedIdx();
            break;
        case R:
            menu.incSelectedIdx();
            break;
        case C:
        case B:
            // Center button or button B will move to child screen
            if (menu.getSelectedItem().toString().equals("Hello")) {
                // Open new HelloScreen screen, with this as the parent
                // (that screen can call goToParentScreen() to return here)
                setCurrScreen(new HelloScreen(this));
            } else if (menu.getSelectedItem().toString().equals("Goodbye")) {
                // Open new GoodbyeScreen screen, with this as the parent
                setCurrScreen(new GoodbyeScreen(this));
            }
            break;
        }
    }
}
```



### Code used in this project:

* OLED driver code and 4x5/5x8 fonts from [Pi-OLED](https://github.com/entrusc/Pi-OLED) by Florian Frankenberger, which is a port of
the [Adafruit_SSD1306](https://github.com/adafruit/Adafruit_SSD1306) Python driver for the SSD1306 OLED driver.
* [Latin1/Korean 16x16](https://github.com/Dalgona/neodgm/blob/master/font.py) font by Dalgona. 
