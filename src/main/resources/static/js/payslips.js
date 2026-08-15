const form = document.getElementById("uploadForm");
const fileInput = document.getElementById("files");

const result = document.getElementById("result");

const resultsSection =
    document.getElementById("resultsSection");

const resultsSummary =
    document.getElementById("resultsSummary");

const resultsTable =
    document.getElementById("resultsTable");

const MAX_FILES = 3;

/*
        Upload
 */
form.addEventListener("submit", async function(event) {

    event.preventDefault();

    const files = fileInput.files;

    // -------------------------
    // Validation
    // -------------------------

    if (files.length === 0) {

        showError("Please select at least one PDF file.");

        return;
    }

    if (files.length > MAX_FILES) {

        showError(
            `You can upload a maximum of ${MAX_FILES} payslips at once.`
        );

        return;
    }

    for (const file of files) {

        if (file.type !== "application/pdf") {

            showError(
                `"${file.name}" is not a PDF file.`
            );

            return;
        }
    }


    // -------------------------
    // Prepare files
    // -------------------------

    const formData = new FormData();

    for (const file of files) {

        formData.append("files", file);
    }


    // -------------------------
    // Processing message
    // -------------------------

    result.innerHTML = `
        <div class="alert alert-info mt-4">
            <strong>Processing payslips...</strong><br>
            Depending on the size and number of files,
            this may take a few minutes.
        </div>
    `;

    resultsSection.classList.add("d-none");


    try {

        const response = await fetch(
            "/api/payslip/upload",
            {
                method: "POST",
                body: formData
            }
        );


        if (!response.ok) {

            const message = await response.text();

            throw new Error(message);
        }


        const data = await response.json();

        console.log("Backend response:", data);

        result.textContent = "";

        displayResults(data);


        // -------------------------
        // Display results
        // -------------------------

        displayResults(data);

        result.innerHTML = "";


    } catch (error) {

        console.error(error);

        result.innerHTML = `
            <div class="alert alert-danger mt-4">
                Error: ${error.message}
            </div>
        `;
    }

});

/*
        Generating table
 */
function displayResults(data) {

    console.log("Results received:", data);

    // Your backend should return an array
    // of PayslipResult objects.

    const resultsSection = document.getElementById("resultsSection");
    const tableHead = document.getElementById("resultsTableHead");
    const tableBody = document.getElementById("resultsTableBody");

    console.log("resultsSection:", resultsSection);
    console.log("tableHead:", tableHead);
    console.log("tableBody:", tableBody);

    tableHead.innerHTML = "";
    tableBody.innerHTML = "";

    const results = Array.isArray(data)
        ? data
        : data.results;


    if (!results || results.length === 0) {

        resultsSection.classList.add("d-none");

        result.innerHTML = `
            <div class="alert alert-warning mt-4">
                No extraction results were returned.
            </div>
        `;

        return;
    }


    resultsSummary.textContent =
        `${results.length} payslip(s) processed successfully.`;


    buildResultsTable(results);


    resultsSection.classList.remove("d-none");
}

function buildResultsTable(results) {

    const tableHead =
        resultsTable.querySelector("thead");

    const tableBody =
        resultsTable.querySelector("tbody");


    // Clear previous results

    tableHead.innerHTML = "";
    tableBody.innerHTML = "";


    // --------------------------------
    // Create header
    // --------------------------------

    const headerRow =
        document.createElement("tr");


    const fieldHeader =
        document.createElement("th");

    fieldHeader.textContent = "Field";

    headerRow.appendChild(fieldHeader);


    results.forEach((payslip, index) => {

        const th =
            document.createElement("th");

        th.textContent =
            payslip.fileName ??
            `Payslip ${index + 1}`;

        headerRow.appendChild(th);

    });


    tableHead.appendChild(headerRow);


    // --------------------------------
    // Collect all field names
    // --------------------------------

    const fieldNames = new Set();


    results.forEach(payslip => {

        /*
         * This is the part that depends on
         * your actual PayslipResult structure.
         */

        const fields = payslip.extractionResults;

        if (fields) {

            Object.keys(fields).forEach(field => {

                fieldNames.add(field);

            });
        }

    });


    // --------------------------------
    // Create rows
    // --------------------------------

    fieldNames.forEach(fieldName => {

        const row =
            document.createElement("tr");


        // Field name

        const fieldCell =
            document.createElement("th");

        fieldCell.textContent = fieldName;

        row.appendChild(fieldCell);


        // Values for each payslip

        results.forEach(payslip => {

            const cell =
                document.createElement("td");

            const fields =
                payslip.extractionResults;


            if (fields && fields[fieldName] !== undefined) {

                cell.textContent =
                    fields[fieldName];

            } else {

                cell.textContent = "—";

                cell.classList.add(
                    "text-muted"
                );
            }


            row.appendChild(cell);

        });


        tableBody.appendChild(row);

    });
}

function displayResults(data) {

    const resultsSection = document.getElementById("resultsSection");
    const tableHead = document.getElementById("resultsTableHead");
    const tableBody = document.getElementById("resultsTableBody");

    // Clear previous results
    tableHead.innerHTML = "";
    tableBody.innerHTML = "";

    if (!data || data.length === 0) {
        resultsSection.classList.add("d-none");
        return;
    }

    // -------------------------------------------------
    // Create table header
    // -------------------------------------------------

    let headerRow = "<tr>";
    headerRow += "<th>Field</th>";

    data.forEach(result => {
        headerRow += `<th>${result.fileName}</th>`;
    });

    headerRow += "</tr>";

    tableHead.innerHTML = headerRow;


    // -------------------------------------------------
    // Get all field names
    // -------------------------------------------------

    const fieldNames = new Set();

    data.forEach(result => {

        if (result.fields) {

            Object.keys(result.fields).forEach(fieldName => {
                fieldNames.add(fieldName);
            });

        }

    });


    // -------------------------------------------------
    // Create table rows
    // -------------------------------------------------

    fieldNames.forEach(fieldName => {

        let row = "<tr>";

        // First column = field name
        row += `<th>${fieldName}</th>`;

        // Remaining columns = values from each payslip
        data.forEach(result => {

            const value = result.fields
                ? result.fields[fieldName]
                : null;

            row += `<td>${value ?? ""}</td>`;
        });

        row += "</tr>";

        tableBody.innerHTML += row;
    });


    // Show table
    resultsSection.classList.remove("d-none");
}