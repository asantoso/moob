<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
xmlns:fn="http://www.w3.org/2005/xpath-functions"

>
	<xsl:output method="text"/>
	<xsl:template name="declarevariables">
		<xsl:param name="nodes"/>
		
		<xsl:for-each select="$nodes/.">
			<xsl:text>String </xsl:text>
			<xsl:call-template name="genVarName"><xsl:with-param name="nodename" select="fn:local-name(.)"></xsl:with-param></xsl:call-template>			
			<xsl:text>="";</xsl:text>
		</xsl:for-each>
		
		<xsl:for-each select="$nodes/.">
			<xsl:text>boolean </xsl:text>
			<xsl:call-template name="genFlagVarName"><xsl:with-param name="nodename" select="fn:local-name(.)"></xsl:with-param></xsl:call-template>			
			<xsl:text>=false;</xsl:text>
		</xsl:for-each>
		
	</xsl:template>
	
	<xsl:template name="genVarName">
		<xsl:param name="nodename"/>
		<xsl:text></xsl:text><xsl:value-of select="$nodename"/>
	</xsl:template>
	
	<xsl:template name="genFlagVarName">
		<xsl:param name="nodename"/>
		<xsl:text>_</xsl:text><xsl:value-of select="$nodename"/>
	</xsl:template>
	
	<xsl:template name="start_tag">
		<xsl:param name="nodes"/>
		<xsl:text>
		if (eventType == XmlPullParser.START_TAG) {
Util.logi("xpp", "Start tag");</xsl:text>
	String tn = xpp.getName();
		<xsl:for-each select="$nodes">
			<xsl:variable name="nodename" select="fn:local-name(.)" />
			<xsl:variable name="flagVar">
					<xsl:call-template name="genFlagVarName">
				<xsl:with-param name="nodename" select="$nodename"/>
				</xsl:call-template>
			</xsl:variable>
			  <xsl:choose>
				  <xsl:when test="fn:position(.) &gt; 1">else</xsl:when>
					<xsl:otherwise></xsl:otherwise>
			  </xsl:choose>
			if(tn.compareTo("<xsl:value-of select="$nodename" />") == 0){
				<xsl:value-of select="$flagVar" />=true;
			}
		</xsl:for-each>
	
		<xsl:text>
		}</xsl:text>
	</xsl:template>
	
	
	<xsl:template name="end_tag">
		<xsl:param name="nodes"/>
		<xsl:text>
		if (eventType == XmlPullParser.END_TAG) {
Util.logi("xpp", "End tag");</xsl:text>
String tn = xpp.getName();
		<xsl:for-each select="$nodes">
			<xsl:variable name="nodename" select="fn:local-name(.)" />
			<xsl:variable name="flagVar">
					<xsl:call-template name="genFlagVarName">
				<xsl:with-param name="nodename" select="$nodename"/>
				</xsl:call-template>
			</xsl:variable>
			  <xsl:choose>
				  <xsl:when test="fn:position(.) &gt; 1">else</xsl:when>
					<xsl:otherwise></xsl:otherwise>
			  </xsl:choose>
			if(tn.compareTo("<xsl:value-of select="$nodename" />") == 0){
				<xsl:value-of select="$flagVar" />=false;
			}
		</xsl:for-each>
	
		<xsl:text>
		}</xsl:text>
	</xsl:template>
	
	
		
	<xsl:template name="node_text">
		<xsl:param name="nodes"/>
		<xsl:text>
		if (eventType == XmlPullParser.TEXT) {
		Util.logi("xpp", "Text");
		String v = xpp.getText();	
		Util.logi("xpp", "Text " + v);
		</xsl:text>

		<xsl:for-each select="$nodes">
			<xsl:variable name="nodename" select="fn:local-name(.)" />
			<xsl:variable name="flagVar">
					<xsl:call-template name="genFlagVarName">
				<xsl:with-param name="nodename" select="$nodename"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:variable name="var">
					<xsl:call-template name="genVarName">
				<xsl:with-param name="nodename" select="$nodename"/>
				</xsl:call-template>
			</xsl:variable>
			
			  <xsl:choose>
				  <xsl:when test="fn:position(.) &gt; 1">else</xsl:when>
					<xsl:otherwise></xsl:otherwise>
			  </xsl:choose>
			if(<xsl:value-of select="$flagVar"/>){
				<xsl:value-of select="$flagVar"/> = false;
				<xsl:value-of select="$var"/> = v;
			}	
		</xsl:for-each>
	
		<xsl:text>
		}</xsl:text>
	</xsl:template>
	
	
	<xsl:template match="/">
		<xsl:variable name="nodes" select="descendant::node() except descendant::text()"/>
		
		<xsl:call-template name="declarevariables">
			<xsl:with-param name="nodes" select="$nodes"/>
		</xsl:call-template>
		
			
		try {
			int eventType = xpp.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				
					<xsl:call-template name="start_tag">
			<xsl:with-param name="nodes" select="$nodes"/>
		</xsl:call-template>
		
		<xsl:call-template name="end_tag">
			<xsl:with-param name="nodes" select="$nodes"/>
		</xsl:call-template>
		
		<xsl:call-template name="node_text">
			<xsl:with-param name="nodes" select="$nodes"/>
		</xsl:call-template>
		
				eventType = xpp.next();
			}
		} catch (XmlPullParserException e) {
			Util.loge("xpp",e.getMessage());
		} catch (IOException e) {
			Util.loge("xpp",e.getMessage());
			e.printStackTrace();
		}
			
	</xsl:template>
</xsl:stylesheet>
