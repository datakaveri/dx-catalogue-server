package iudx.catalogue.server.authenticator.model;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.authenticator.model.DxRole;
import iudx.catalogue.server.authenticator.model.JwtData;
import jdk.jfr.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static junit.framework.Assert.assertNull;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class DxRoleTest {

    @Test
    @Description("testing the method create")
    public void testCreate(VertxTestContext vertxTestContext) {
    String role="dummy";
    JwtData jwtData = new JwtData();
    jwtData.setRole(role);
    assertNull(DxRole.fromRole(jwtData));
    vertxTestContext.completeNow();
    }

}
