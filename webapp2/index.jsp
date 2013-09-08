<%@page contentType="text/html;charset=UTF-8"%><html>
<head>
<title>index.jsp</title>
<script type="text/javascript">
	function showAlert() {
		alert('ok');
	}
	function showConfirm() {
		alert(confirm('confirm?'));
	}
	function showPrompt() {
		alert(prompt('prompt?'));
	}
</script>
</head>
<body>
	<h1>index.jsp</h1>
	<p>
		<a href="simple_server_servlet1">SimpleServerServlet1</a>
	</p>
	<p>
		<a href="page2.jsp" target="POPUP1">page2.jsp</a>
	</p>
	<p>
		<a href="javascript:void(0)" onclick="return showAlert()">show
			alert</a>
	<p>
		<a href="javascript:void(0)" onclick="return showConfirm()">show
			confirm</a>
	<p>
		<a href="javascript:void(0)" onclick="return showPrompt()">show
			prompt</a>
</body>
</html>
