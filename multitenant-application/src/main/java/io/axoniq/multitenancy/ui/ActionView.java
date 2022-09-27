package io.axoniq.multitenancy.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import io.axoniq.multitenancy.web.MessageService;

/**
 * @author Stefan Dragisic
 */
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

        layout.add(new Button("Send queries...", evt -> {
            messageService.sendQueries(tenantName);
            showSuccess(tenantName);
        }));

        layout.add(new Button("Send subscription queries...", evt -> {
            messageService.subscriptionQuery(tenantName).subscribe();
            showSuccess(tenantName);
        }));

        layout.add(new Button("Reset projections...", evt -> {

            showSuccess(tenantName);
        }));

        layout.add(new Button("Explore projection...", evt -> {

            showSuccess(tenantName);
        }));

        layout.setAlignItems(Alignment.CENTER);

        Button register = new Button("Or go back...", event -> UI.getCurrent().navigate(""));
        register.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        layout.add(register);

        add(layout);
    }

    private void showSuccess(String tenantName) {
        Notification notification = Notification.show("Action done! Check Axon Server dashboard and stats/events for context: " + tenantName);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
