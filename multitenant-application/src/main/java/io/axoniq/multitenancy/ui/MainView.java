package io.axoniq.multitenancy.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.axonframework.common.Registration;
import org.axonframework.extensions.multitenancy.components.MultiTenantAwareComponent;
import org.axonframework.extensions.multitenancy.components.TenantDescriptor;
import org.axonframework.extensions.multitenancy.components.TenantProvider;

import java.util.stream.Collectors;

/**
 * @author Stefan Dragisic
 */
@Route
public class MainView extends VerticalLayout  {

    public MainView(TenantProvider tenantProvider) {
        VerticalLayout layout = new VerticalLayout();

        H1 title = new H1("Login");
        layout.add(title);

        layout.add(new Text("Available tenants:"));

        tenantProvider
                .getTenants()
                .stream()
                .map(TenantDescriptor::tenantId)
                .map(name -> new Button(name, event -> UI.getCurrent().navigate("action/"+name)))
                .forEach(layout::add);

        layout.setAlignItems(Alignment.CENTER);


        Button register = new Button("Or register new...", event -> UI.getCurrent().navigate("register"));
        register.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        layout.add(register);

        add(layout);
    }



}
