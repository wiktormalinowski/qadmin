package pl.maliniak.qadmin.runtime;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/q/qadmin/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    AdminService adminService;

    @POST
    @Path("/ai/query")
    public Map<String, Object> queryAi(PromptRequest promptRequest) {
        Map<String, Object> view = adminService.queryAi(promptRequest);
        if (view == null) {
            throw new NotFoundException("Entity not found for given prompt");
        }
        return view;
    }

    @DELETE
    @Path("/entities/{entityName}/{id}")
    public Response deleteData(@PathParam("entityName") String entityName, @PathParam("id") String id) {
        try {
            boolean deleted = adminService.deleteData(entityName, id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}