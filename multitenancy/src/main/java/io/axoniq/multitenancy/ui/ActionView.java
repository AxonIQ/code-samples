package io.axoniq.multitenancy.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import io.axoniq.multitenancy.web.MessageService;

@Route("action")
public class ActionView extends VerticalLayout implements HasUrlParameter<String> {

    private final MessageService messageService;

    public ActionView(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, String tenantName) {
        VerticalLayout layout = new VerticalLayout();

        H1 title = new H1("Actions (" + tenantName + ")");
        layout.add(title);

        layout.add(new Text("Do something:"));

        layout.add(new Button("Send commands...", evt -> {
            messageService.sendCommands(tenantName);
            showSuccess(tenantName);
        }));


        layout.add(new Button("Publish events...", evt -> {
            messageService.sendEvents(tenantName);
            showSuccess(tenantName);
        }));

        layout.add(new Button("Schedule events...", evt -> {
            messageService.scheduleEvents(tenantName);
            showSuccess(tenantName);
        }));

        layout.add(new Button("Send queries...", evt -> {
            messageService.sendQueries(tenantName);
            showSuccess(tenantName);
        }));

        layout.add(new Button("Send subscription query...", evt -> {
            messageService.subscriptionQuery(tenantName).block();
            showSuccess(tenantName);
        }));

        layout.add(new Button("Explore projections...", evt -> {
            Dialog dialog = new Dialog();

            dialog.setHeaderTitle("Explore projections");

            Span name = new Span("JDBC URL: jdbc:h2:mem:all-tenants");
            Span email = new Span("username: sa");

            VerticalLayout content = new VerticalLayout(name, email);
            content.setSpacing(false);
            content.setPadding(false);

            Details details = new Details("Login details", content);
            details.setOpened(true);
            dialog.add(content);
            Button goButton = new Button("Open DB",
                                         e -> UI.getCurrent().getPage()
                                                .open("http://localhost:8080/h2-console", "_blank"));
            Button cancelButton = new Button("Close", e -> dialog.close());
            dialog.getFooter().add(goButton);
            dialog.getFooter().add(cancelButton);
            dialog.open();
        }));

        layout.setAlignItems(Alignment.CENTER);

        Button register = new Button("Or go back...", event -> UI.getCurrent().navigate(""));
        register.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        layout.add(register);

        add(layout);
    }

    private void showSuccess(String tenantName) {
        Notification notification = Notification.show(
                "Action done! Check Axon Server dashboard and stats/events for context: " + tenantName
        );
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
