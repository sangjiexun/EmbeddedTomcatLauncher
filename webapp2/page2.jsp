<%@page contentType="text/html;charset=UTF-8"%><html>
<head>
<title>page2</title>
<script type="text/javascript">
	function showAlertOnParent() {
<%-- このウィンドウを開いた親のJavaScriptを呼び出してみる --%>
	window.opener.showAlert();
	}
</script>
</head>
<body>
	<h1>page2.jsp</h1>
	<p>
		window name=
		<script type="text/javascript">
			document.write(window.name);
		</script>
	</p>

	<input type="BUTTON" onclick="showAlertOnParent()" value="PARENT ALERT">
	<p>
		<a href="index.jsp">index.jsp</a>
	</p>
</body>
</html>
