<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml">
  
  <xsl:template match="template-search">
    <form name="templateSearchForm" method="get" action="{xsl:concat($webappprefix,'/templateSearch.do')}">
        Search:
        <input type="text" name="queryString" size="20" value="" id="queryString" />
        <select name="type"><option value="global" selected="selected">Public templates</option>
          <option value="user">My templates</option>
          <option value="ALL">Everything</option></select>
        <input type="submit" value="Search" />
        <br/>
      </form>
  </xsl:template>
  
</xsl:stylesheet>
