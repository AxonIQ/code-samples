package io.axoniq.multitenancy.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.axoniq.multitenancy.web.CreateTenantService;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Stefan Dragisic
 */
@Route("/register")
public class RegisterView extends VerticalLayout {

    private final CreateTenantService createTenantService;

    public RegisterView(CreateTenantService createTenantService) {
        this.createTenantService = createTenantService;
        H1 title = new H1("Register tenant");

        TextField tenantName = new TextField("Tenant name");


        FormLayout formLayout = new FormLayout();
        formLayout.add(title);


        formLayout.add(tenantName);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0px", 2));


        formLayout.setColspan(title, 2);
        formLayout.setColspan(tenantName, 2);

        formLayout.setWidth("50%");

        Button submitButton = new Button("Register");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        formLayout.add(submitButton);
        formLayout.setColspan(submitButton, 2);
        submitButton.getStyle().set("padding", "25px");
        add(formLayout);


        submitButton.addClickListener(e -> {
            String tenantNameValue = tenantName.getValue();

            createTenantService
                    .createTenant("tenant-"+tenantNameValue,"default",true)
                    .exceptionally(err -> {
                        showError(err.getMessage());
                        return null;
                    }).join();

            showSuccess(tenantNameValue);

            try {
                Thread.sleep(2500);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            new Button("redirect", event -> UI.getCurrent().navigate("")).click();
        });
    }

    private void showSuccess(String tenantName) {
        Notification notification = Notification.show("Tenant created with username: tenant-" +tenantName);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String error) {
        Notification notification = Notification.show("Error has occurred: " +error);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }



}
