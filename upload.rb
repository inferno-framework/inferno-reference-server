require 'httparty'
require 'pry'

FHIR_SERVER = 'http://localhost:8080/reference-server/r4'
TOKEN = 'SAMPLE_TOKEN'

def upload_us_core_resources
    entries = []
    exempt_resource_types = ["CapabilityStatement", "CodeSystem", "ConceptMap", "ImplementationGuide", "OperationDefinition", "SearchParameter", "StructureDefinition", "ValueSet"]

    file_path = File.join(__dir__, 'us-core-r4-resources', '*.json')
    filenames = Dir.glob(file_path)
                .select { |filename| filename.end_with? '.json' }
      
    resources_to_upload_separately = []
    filenames.each do |filename|
      resource = JSON.parse(File.read(filename), symbolize_names: true)
      puts "Adding #{filename} to transaction"

      if resource[:type] == 'transaction'
        entries = entries.concat(resource[:entry])
      elsif (exempt_resource_types.include? resource[:resourceType])
        resources_to_upload_separately.push(resource)
      else
        #wrap resource in entry
        fullUrl = "urn:uuid:" + resource[:id]
        resourceType = resource[:resourceType]

        entry = {
          "fullUrl": fullUrl,
          "resource": resource,
          "request": {
            "method": "POST",
            "url": resourceType
          }

        }
        entries.push(entry)
      end
    end

    transactionJson = {
        "type": "transaction",
        "entry": entries,
        "resourceType": "Bundle",        
    }

    response = execute_transaction(transactionJson)

    if !response.success?
      puts "Error uploading transaction because: #{response.body}"
    end


    old_retry_count = resources_to_upload_separately.length
    loop do
      resources_to_retry = []
      resources_to_upload_separately.each do |resource|
        response = upload_resource(resource)
        if !response.success?
            puts "Error uploading #{resource}: #{response.body}"
            resources_to_retry.push(resource)
        end
      end

      break if resources_to_retry.empty?

      retry_count = resources_to_retry.length
      if retry_count == old_retry_count
        puts "Unable to upload #{retry_count} resources:"
        puts resources_to_upload_separately.join("\n")
        break
      end
      puts "#{retry_count} resources to retry"
      resources_to_upload_separately = resources_to_retry
      old_retry_count = retry_count
    end
end

def upload_resource(resource)
  resource_type = resource[:resourceType]
  id = resource[:id]
  HTTParty.put(
    "#{FHIR_SERVER}/#{resource_type}/#{id}",
    body: resource.to_json,
    headers: {
     'Content-Type': 'application/json',
     'Authorization': "Bearer #{TOKEN}"
    }
  )
end

def patient_identifier_in_transaction(transaction)
  patient_record = transaction[:entry]&.find {|r| r[:resource][:resourceType] == 'Patient'}
  identifier = patient_record[:resource][:identifier].first
  "#{identifier[:system]}|#{identifier[:value]}"
end

def record_exists_on_server?(patient_identifier)
  response = HTTParty.get(
    "#{FHIR_SERVER}/Patient",
    query: { identifier: patient_identifier },
    headers: {
      'Content-Type': 'application/json',
      'Authorization': "Bearer #{TOKEN}"
     }
  )
  JSON.parse(response.body)['entry']&.any?
end

def execute_transaction(transaction)
  HTTParty.post(
    FHIR_SERVER,
    body: transaction.to_json,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': "Bearer #{TOKEN}"
     },
     timeout: 600
  )
end

upload_us_core_resources