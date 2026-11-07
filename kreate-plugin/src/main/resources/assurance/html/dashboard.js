(function(){var search=document.getElementById("search");var boxes=document.querySelectorAll("input[data-severity]");var rows=document.querySelectorAll("tr[data-severity]");
function apply(){var term=(search&&search.value||"").toLowerCase();var shown={};boxes.forEach(function(box){shown[box.getAttribute("data-severity")]=box.checked;});
rows.forEach(function(row){var visible=shown[row.getAttribute("data-severity")]!==false&&row.textContent.toLowerCase().indexOf(term)>=0;row.classList.toggle("hidden",!visible);});}
if(search){search.addEventListener("input",apply);}boxes.forEach(function(box){box.addEventListener("change",apply);});})();
