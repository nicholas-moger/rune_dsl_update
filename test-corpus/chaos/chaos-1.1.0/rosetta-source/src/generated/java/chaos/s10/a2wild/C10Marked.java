package chaos.s10.a2wild;

import chaos.s10.a2wild.h.C10Aux;
import chaos.s10.a2wild.meta.C10MarkedMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Annotated type - bare ref, attribute-selecting ref, prefixed ref.
 * @version 1.0.0
 */
@RosettaDataType(value="C10Marked", builder=C10Marked.C10MarkedBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C10Marked", model="chaos", builder=C10Marked.C10MarkedBuilderImpl.class, version="1.0.0")
public interface C10Marked extends RosettaModelObject {

	C10MarkedMeta metaData = new C10MarkedMeta();

	/*********************** Getter Methods  ***********************/
	String getMid();
	C10Aux getAux();

	/*********************** Build Methods  ***********************/
	C10Marked build();
	
	C10Marked.C10MarkedBuilder toBuilder();
	
	static C10Marked.C10MarkedBuilder builder() {
		return new C10Marked.C10MarkedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C10Marked> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C10Marked> getType() {
		return C10Marked.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("mid"), String.class, getMid(), this);
		processRosetta(path.newSubPath("aux"), processor, C10Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C10MarkedBuilder extends C10Marked, RosettaModelObjectBuilder {
		C10Aux.C10AuxBuilder getOrCreateAux();
		@Override
		C10Aux.C10AuxBuilder getAux();
		C10Marked.C10MarkedBuilder setMid(String mid);
		C10Marked.C10MarkedBuilder setAux(C10Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("mid"), String.class, getMid(), this);
			processRosetta(path.newSubPath("aux"), processor, C10Aux.C10AuxBuilder.class, getAux());
		}
		

		C10Marked.C10MarkedBuilder prune();
	}

	/*********************** Immutable Implementation of C10Marked  ***********************/
	class C10MarkedImpl implements C10Marked {
		private final String mid;
		private final C10Aux aux;
		
		protected C10MarkedImpl(C10Marked.C10MarkedBuilder builder) {
			this.mid = builder.getMid();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("mid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("mid")
		public String getMid() {
			return mid;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C10Aux getAux() {
			return aux;
		}
		
		@Override
		public C10Marked build() {
			return this;
		}
		
		@Override
		public C10Marked.C10MarkedBuilder toBuilder() {
			C10Marked.C10MarkedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C10Marked.C10MarkedBuilder builder) {
			ofNullable(getMid()).ifPresent(builder::setMid);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C10Marked _that = getType().cast(o);
		
			if (!Objects.equals(mid, _that.getMid())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (mid != null ? mid.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C10Marked {" +
				"mid=" + this.mid + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C10Marked  ***********************/
	class C10MarkedBuilderImpl implements C10Marked.C10MarkedBuilder {
	
		protected String mid;
		protected C10Aux.C10AuxBuilder aux;
		
		@Override
		@RosettaAttribute("mid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("mid")
		public String getMid() {
			return mid;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C10Aux.C10AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C10Aux.C10AuxBuilder getOrCreateAux() {
			C10Aux.C10AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C10Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("mid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("mid")
		@Override
		public C10Marked.C10MarkedBuilder setMid(String _mid) {
			this.mid = _mid == null ? null : _mid;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C10Marked.C10MarkedBuilder setAux(C10Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C10Marked build() {
			return new C10Marked.C10MarkedImpl(this);
		}
		
		@Override
		public C10Marked.C10MarkedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C10Marked.C10MarkedBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getMid()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C10Marked.C10MarkedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C10Marked.C10MarkedBuilder o = (C10Marked.C10MarkedBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(getMid(), o.getMid(), this::setMid);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C10Marked _that = getType().cast(o);
		
			if (!Objects.equals(mid, _that.getMid())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (mid != null ? mid.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C10MarkedBuilder {" +
				"mid=" + this.mid + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}
