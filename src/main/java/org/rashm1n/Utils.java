package org.rashm1n;

public class Utils {
    public static String getDeleteComponentMutation(String componentId, String orgHandler, String projectId) {
        return "mutation{ deleteComponentV2(" +
                "        orgHandler: \"" + orgHandler + "\"," +
                "        projectId: \"" + projectId + "\",\n" +
                "        componentId: \"" + componentId + "\"){status, canDelete, message" +
                "}}";
    }

    public static String getComponentsQuery(String orgHandler, String projectId) {

        return "query{" +
                "      components(" +
                "        orgHandler: \"" + orgHandler + "\"," +
                "        projectId: \"" + projectId + "\"" +
                "      ){\n" +
                "        projectId," +
                "        id," +
                "        description," +
                "        name," +
                "        handler," +
                "        displayName," +
                "        displayType," +
                "        version," +
                "        createdAt," +
                "        orgHandler" +
                "      }" +
                "    }";
    }
}
